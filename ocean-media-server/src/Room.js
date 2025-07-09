const Recorder = require('./recorder');

class Room {
  constructor(roomId, workspaceId, router) {
    this.roomId = roomId;
    this.workspaceId = workspaceId;
    this.router = router;
    this.peers = new Map();
    this.hostId = null; // 호스트 ID 추가
    this.status = 'WAITING'; // 회의 상태 추가
    this.startTime = null;
    this.endTime = null;
    this.recordingStatus = false;
    this.recorder = null;
    this.sharedFiles = [];
  }

  // 호스트 설정
  setHost(userId) {
    this.hostId = userId;
    this.status = 'IN_PROGRESS';
    this.startTime = new Date();
    console.log(`Room ${this.roomId}: 호스트 설정 - ${userId}`);
  }

  // 호스트 확인
  isHost(userId) {
    return this.hostId === userId;
  }

  // 호스트 권한 이전
  transferHost(newHostId) {
    const newHost = this.getPeerByUserId(newHostId);
    if (!newHost) {
        throw new Error('새 호스트를 찾을 수 없습니다');
    }

    const oldHostId = this.hostId;
    this.hostId = newHostId;

    // 역할 업데이트
    this.peers.forEach(peer => {
       if (peer.userId === oldHostId) {
            peer.role = 'PARTICIPANT';
       } else if (peer.userId === newHostId) {
           peer.role = 'HOST';
       }
    });

    console.log(`Room ${this.roomId}: 호스트 권한 이전 ${oldHostId} -> ${newHostId}`);
        return { oldHostId, newHostId };
  }

  // userId로 Peer 찾기
  getPeerByUserId(userId) {
    for (const [peerId, peer] of this.peers) {
       if (peer.userId === userId) {
          return peer;
       }
    }
      return null;
  }

  // 회의 종료
  endMeeting() {
    this.status = 'ENDED';
    this.endTime = new Date();

    // 녹화 중이면 중지
    if (this.recordingStatus && this.recorder) {
        this.recorder.stop();
    }

      console.log(`Room ${this.roomId}: 회의 종료됨`);
  }

  // 활성 참가자 수 확인
  getActiveParticipants() {
    let count = 0;
    this.peers.forEach(peer => {
      if (peer.isActive !== false) {
          count++;
      }
    });
      return count;
    }

  // 녹화 시작 - 완전 수정 버전
  async startRecording(recorderId, recordingPath) {
      if (this.recordingStatus) {
          throw new Error('이미 녹화 중입니다');
      }

      try {
          // Producer 찾기
          const videoProducer = this.getVideoProducer();
          const audioProducer = this.getAudioProducer();

          console.log('비디오 Producer:', videoProducer ? '있음' : '없음');
          console.log('오디오 Producer:', audioProducer ? '있음' : '없음');

          if (!videoProducer && !audioProducer) {
              throw new Error('녹화할 미디어 스트림이 없습니다');
          }

          // Transport 옵션
          // Transport 옵션
          const transportOptions = {
              listenIp: {
                  ip: '127.0.0.1',
                  announcedIp: null
              },
              rtcpMux: false,
              comedia: true,  // ⭐ false → true로 변경! 중요!
              enableSctp: false,
              enableSrtp: false
          };

          // Transport 생성
          const videoTransport = await this.router.createPlainTransport(transportOptions);
          const audioTransport = await this.router.createPlainTransport(transportOptions);

          // ⭐ 실제 Transport 포트 사용
          const ffmpegVideoPort = videoTransport.tuple.localPort;
          const ffmpegAudioPort = audioTransport.tuple.localPort;

          console.log('비디오 Transport 정보:', {
              id: videoTransport.id,
              port: ffmpegVideoPort,
              rtcpPort: videoTransport.rtcpTuple ? videoTransport.rtcpTuple.localPort : 'N/A'
          });

          console.log('오디오 Transport 정보:', {
              id: audioTransport.id,
              port: ffmpegAudioPort,
              rtcpPort: audioTransport.rtcpTuple ? audioTransport.rtcpTuple.localPort : 'N/A'
          });

          // ⭐ 중요: Recorder 인스턴스 생성
          this.recorder = new Recorder(
              this.roomId,
              this.workspaceId,
              recorderId,
              process.env.SPRING_BOOT_URL || 'http://localhost:8080',
              recordingPath
          );

          // Consumer 생성
          const videoConsumer = videoProducer ? await videoTransport.consume({
              producerId: videoProducer.id,
              rtpCapabilities: this.router.rtpCapabilities,
              paused: false
          }) : null;

          const audioConsumer = audioProducer ? await audioTransport.consume({
              producerId: audioProducer.id,
              rtpCapabilities: this.router.rtpCapabilities,
              paused: false
          }) : null;

          // RTP Parameters 준비
          const videoRtpParams = videoConsumer ? videoConsumer.rtpParameters : null;
          const audioRtpParams = audioConsumer ? audioConsumer.rtpParameters : null;

          // ⭐ 녹화 시작
          console.log('Recorder.startRecording 호출 중...');
          const result = await this.recorder.startRecording(
              ffmpegVideoPort,
              ffmpegAudioPort,
              videoRtpParams,
              audioRtpParams
          );
          console.log('Recorder 시작 결과:', result);

          // Consumer resume 이후 부분
          if (videoConsumer) {
              await videoConsumer.resume();
              console.log('✅ 비디오 Consumer resumed');
          }

          if (audioConsumer) {
              await audioConsumer.resume();
              console.log('✅ 오디오 Consumer resumed');
          }

          // ⭐ GStreamer가 시작되길 기다림
          await new Promise(resolve => setTimeout(resolve, 2000));

          // ⭐ Transport를 명시적으로 연결 (comedia: true여도 필요할 수 있음)
          if (videoTransport && videoConsumer) {
              console.log('비디오 Transport 연결 시도...');
              await videoTransport.connect({
                  ip: '127.0.0.1',
                  port: ffmpegVideoPort
              });
              console.log('✅ 비디오 Transport 연결됨');
          }

          if (audioTransport && audioConsumer) {
              console.log('오디오 Transport 연결 시도...');
              await audioTransport.connect({
                  ip: '127.0.0.1',
                  port: ffmpegAudioPort
              });
              console.log('✅ 오디오 Transport 연결됨');
          }

          // ⭐ Transport 통계 모니터링 추가
          const checkTransportStats = async () => {
              if (!this.recordingStatus) return;

              try {
                  if (videoTransport && !videoTransport.closed) {
                      const stats = await videoTransport.getStats();
                      console.log('📊 비디오 Transport 통계:', stats);

                      if (videoConsumer && !videoConsumer.closed) {
                          const consumerStats = await videoConsumer.getStats();
                          console.log('📊 비디오 Consumer 통계:', consumerStats);
                      }
                  }

                  if (audioTransport && !audioTransport.closed) {
                      const stats = await audioTransport.getStats();
                      console.log('📊 오디오 Transport 통계:', stats);

                      if (audioConsumer && !audioConsumer.closed) {
                          const consumerStats = await audioConsumer.getStats();
                          console.log('📊 오디오 Consumer 통계:', consumerStats);
                      }
                  }
              } catch (error) {
                  console.error('Transport 통계 조회 오류:', error);
              }
          };

          // 처음 한 번 즉시 확인
          await checkTransportStats();

          // 5초마다 통계 확인
          this._statsInterval = setInterval(checkTransportStats, 5000);

          // 상태 저장
          this.recordingStatus = true;
          this.recordingTransports = {
              videoTransport,
              audioTransport,
              videoConsumer,
              audioConsumer
          };

          // 성공 반환
          return {
              success: true,
              recordingId: result.recordingId,
              filePath: result.filePath
          };

      } catch (error) {
          // ⭐ 여기는 에러 처리 부분 (catch 블록)
          console.error('녹화 시작 실패:', error);

          // 정리
          if (this.recordingTransports) {
              if (this.recordingTransports.videoTransport) {
                  this.recordingTransports.videoTransport.close();
              }
              if (this.recordingTransports.audioTransport) {
                  this.recordingTransports.audioTransport.close();
              }
          }

          this.recordingTransports = null;
          this.recordingStatus = false;
          this.recorder = null;

          throw error;  // 에러를 다시 throw
      }
  }

  // Room.js의 stopRecording 메서드
  async stopRecording() {
      if (!this.recordingStatus || !this.recorder) {
          throw new Error('녹화 중이 아닙니다');
      }

      try {
          // 통계 모니터링 중지
          if (this._statsInterval) {
              clearInterval(this._statsInterval);
              this._statsInterval = null;
          }

          const result = await this.recorder.stopRecording();

          // Transport 정리
          if (this.recordingTransports) {
              if (this.recordingTransports.videoTransport) {
                  this.recordingTransports.videoTransport.close();
              }
              if (this.recordingTransports.audioTransport) {
                  this.recordingTransports.audioTransport.close();
              }
          }

          this.recordingStatus = false;
          this.recorder = null;
          this.recordingTransports = null;

          return result;

      } catch (error) {
          console.error('녹화 종료 실패:', error);
          throw error;
      }
  }

  // 첫 번째 비디오 Producer 가져오기
  getVideoProducer() {
    for (const peer of this.peers.values()) {
      if (peer.producers && peer.producers.size > 0) {
        for (const producer of peer.producers.values()) {
          if (producer.kind === 'video' && !producer.closed) {
            // Producer가 paused 상태면 재개
            if (producer.paused) {
                console.log('비디오 Producer가 일시정지 상태, 재개 시도...');
                producer.resume();
            }

            // ⭐ Producer 상태 로그
            console.log('선택된 비디오 Producer:', {
                id: producer.id,
                paused: producer.paused,
                type: producer.type,
                appData: producer.appData
            });

            return producer;
          }
        }
      }
    }
    return null;
  }

  // 첫 번째 오디오 Producer 가져오기
  getAudioProducer() {
      for (const peer of this.peers.values()) {
          if (peer.producers && peer.producers.size > 0) {
              for (const producer of peer.producers.values()) {
                  if (producer.kind === 'audio' && !producer.closed) {
                      // Producer가 paused 상태면 재개
                      if (producer.paused) {
                          console.log('오디오 Producer가 일시정지 상태, 재개 시도...');
                          producer.resume();
                      }

                      // ⭐ Producer 상태 로그
                      console.log('선택된 오디오 Producer:', {
                          id: producer.id,
                          paused: producer.paused,
                          type: producer.type,
                          appData: producer.appData
                      });

                      return producer;
                  }
              }
          }
      }
      return null;
  }

  // 모든 Producer 상태 출력
  debugProducers() {
      console.log('\n=== Producer 상태 디버깅 ===');
      let producerCount = 0;

      for (const [peerId, peer] of this.peers.entries()) {
          console.log(`\nPeer: ${peerId}`);
          
          // Transport 상태 확인
          if (peer.transports && peer.transports.size > 0) {
              console.log('  Transports:');
              for (const [transportId, transport] of peer.transports.entries()) {
                  console.log(`    Transport ${transportId}:`);
                  console.log(`      - ICE State: ${transport.iceState}`);
                  console.log(`      - DTLS State: ${transport.dtlsState}`);
                  console.log(`      - Connected: ${transport.iceState === 'connected' && transport.dtlsState === 'connected'}`);
              }
          }

          if (peer.producers && peer.producers.size > 0) {
              for (const [producerId, producer] of peer.producers.entries()) {
                  producerCount++;
                  console.log(`  Producer ${producerId}:`);
                  console.log(`    - Kind: ${producer.kind}`);
                  console.log(`    - Paused: ${producer.paused}`);
                  console.log(`    - Closed: ${producer.closed}`);
                  console.log(`    - Score: ${JSON.stringify(producer.score)}`);

                  // Producer 통계 가져오기
                  producer.getStats().then(stats => {
                      console.log(`    - Stats: ${JSON.stringify(stats)}`);
                  }).catch(err => {
                      console.error(`    - Stats 에러: ${err.message}`);
                  });
              }
          } else {
              console.log('  Producer 없음');
          }
      }

      console.log(`\n총 Producer 수: ${producerCount}`);
      console.log('=========================\n');
  }

  // 녹화 전 사전 체크
  async preRecordingCheck() {
      console.log('\n=== 녹화 사전 체크 ===');

      // 1. Router 상태 확인
      if (!this.router || this.router.closed) {
          console.error('❌ Router가 없거나 닫혀있습니다');
          return false;
      }
      console.log('✅ Router 정상');

      // 2. Peer 확인
      if (this.peers.size === 0) {
          console.error('❌ 참가자가 없습니다');
          return false;
      }
      console.log(`✅ 참가자 수: ${this.peers.size}`);

      // 3. Producer 확인
      const videoProducer = this.getVideoProducer();
      const audioProducer = this.getAudioProducer();

      if (!videoProducer && !audioProducer) {
          console.error('❌ 활성화된 미디어 스트림이 없습니다');
          this.debugProducers();
          return false;
      }
      
      // Transport 연결 상태 확인 - Peer 레벨에서 확인
      let transportConnected = false;
      
      // 모든 Peer의 Transport 확인
      for (const peer of this.peers.values()) {
          if (peer.transports && peer.transports.size > 0) {
              for (const transport of peer.transports.values()) {
                  // producing이 true인 transport 찾기
                  if (transport.appData && transport.appData.producing === true) {
                      // iceState는 'connected' 또는 'completed', dtlsState는 'connected'여야 함
                      if ((transport.iceState === 'connected' || transport.iceState === 'completed') && 
                          transport.dtlsState === 'connected') {
                          transportConnected = true;
                          console.log('✅ Producer Transport 연결됨:', {
                              transportId: transport.id,
                              iceState: transport.iceState,
                              dtlsState: transport.dtlsState
                          });
                          break;
                      } else {
                          console.error('❌ Producer Transport가 아직 연결되지 않았습니다:', {
                              transportId: transport.id,
                              iceState: transport.iceState,
                              dtlsState: transport.dtlsState
                          });
                      }
                  }
              }
              if (transportConnected) break;
          }
      }
      
      if (!transportConnected) {
          console.error('❌ WebRTC Transport가 연결되지 않았습니다. 잠시 후 다시 시도해주세요.');
          return false;
      }

      if (videoProducer) {
          console.log('✅ 비디오 Producer 발견');
          const stats = await videoProducer.getStats();
          console.log('   비디오 통계:', JSON.stringify(stats));
          
          // Producer가 실제로 데이터를 생성하는지 확인
          if (!stats || stats.length === 0 || (stats[0] && stats[0].byteCount === 0)) {
              console.error('❌ 비디오 Producer가 데이터를 생성하지 않습니다!');
              console.log('   Transport 연결 상태를 확인하세요.');
              return false;
          }
      }

      if (audioProducer) {
          console.log('✅ 오디오 Producer 발견');
          const stats = await audioProducer.getStats();
          console.log('   오디오 통계:', JSON.stringify(stats));
          
          // Producer가 실제로 데이터를 생성하는지 확인
          if (!stats || stats.length === 0 || (stats[0] && stats[0].byteCount === 0)) {
              console.error('❌ 오디오 Producer가 데이터를 생성하지 않습니다!');
              console.log('   Transport 연결 상태를 확인하세요.');
              return false;
          }
      }

      // 4. 네트워크 확인 (UDP 포트)
      const checkPort = (port) => {
          return new Promise((resolve) => {
              const dgram = require('dgram');
              const server = dgram.createSocket('udp4');

              server.on('error', (err) => {
                  console.error(`❌ UDP 포트 ${port} 사용 불가: ${err.message}`);
                  resolve(false);
              });

              server.on('listening', () => {
                  console.log(`✅ UDP 포트 ${port} 사용 가능`);
                  server.close();
                  resolve(true);
              });

              server.bind(port);
          });
      };

      const port5004Available = await checkPort(5004);
      const port5006Available = await checkPort(5006);

      if (!port5004Available || !port5006Available) {
          console.error('❌ 필요한 UDP 포트를 사용할 수 없습니다');
          console.log('   다른 프로세스가 포트를 사용 중이거나 권한이 부족할 수 있습니다');
          return false;
      }

      console.log('===================\n');
      return true;
  }

  addPeer(peerId, peer) {
    // Peer 인스턴스를 직접 저장
    this.peers.set(peerId, peer);
    console.log(`Peer ${peerId} joined room ${this.id}`);
    return peer;
  }

  removePeer(peerId) {
    const peer = this.peers.get(peerId);
    if (!peer) return;

    // Peer 클래스의 close 메서드 호출
    if (peer.close && typeof peer.close === 'function') {
      peer.close();
    }

    this.peers.delete(peerId);
    console.log(`Peer ${peerId} left room ${this.id}`);
  }

  getPeer(peerId) {
    return this.peers.get(peerId);
  }

  getAllPeers() {
    return Array.from(this.peers.values());
  }

  isEmpty() {
    return this.peers.size === 0;
  }

  // 참가자 정보 JSON 변환
  toJson() {
    const participants = [];
    this.peers.forEach(peer => {
      participants.push({
      peerId: peer.id,
      userId: peer.userId,
      displayName: peer.displayName,
      role: peer.role || 'PARTICIPANT',
      isActive: peer.isActive !== false,
      joinedAt: peer.joinedAt || new Date()
      });
    });

    return {
       roomId: this.roomId,
       workspaceId: this.workspaceId,
       hostId: this.hostId,
       status: this.status,
       startTime: this.startTime,
       endTime: this.endTime,
       participants: participants,
       participantCount: this.getActiveParticipants(),
       recordingStatus: this.recordingStatus,
       sharedFiles: this.sharedFiles.length
    };
  }

  // 빈 방 확인 (활성 참가자가 없는 경우)
  isEmpty() {
    return this.getActiveParticipants() === 0;
  }
}

module.exports = Room;