// ocean-media-server/src/recorder.js
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const axios = require('axios');
const os = require('os');

class Recorder {
    constructor(roomId, workspaceId, recorderId, springBootUrl, customPath) {
        this.roomId = roomId;
        this.workspaceId = workspaceId;
        this.recorderId = recorderId;
        this.springBootUrl = springBootUrl || 'http://localhost:8080';

        // ⭐ 경로 확장 로직 추가
        if (customPath) {
             this.recordingPath = this.expandPath(customPath);
             console.log('사용자 지정 녹화 경로 (원본):', customPath);
             console.log('사용자 지정 녹화 경로 (확장):', this.recordingPath);
        } else {
             // 기본 경로 사용
             this.recordingPath = process.env.RECORDING_PATH || '/Users/hyunki/Ocean/recordings';
             console.log('기본 녹화 경로:', this.recordingPath);
        }

        this.recordingId = null;
        this.gstreamerProcess = null;
        this.videoPort = null;
        this.audioPort = null;
        this.isRecording = false;
        this.filePath = null;
    }

        /**
         * 경로 확장 메서드 (PathValidationController와 동일한 로직)
         */
        expandPath(inputPath) {
            let expandedPath = inputPath;

            // 홈 디렉토리 치환
            if (expandedPath.startsWith('~')) {
                const homeDir = os.homedir();
                expandedPath = path.join(homeDir, expandedPath.substring(1));
            }

            // 일반적인 디렉토리 이름만 입력한 경우
            const commonDirs = ['Desktop', 'Documents', 'Downloads', 'Pictures', 'Movies', 'Music'];
            for (const dir of commonDirs) {
                if (expandedPath === dir || expandedPath === `/${dir}`) {
                    const homeDir = os.homedir();
                    return path.join(homeDir, dir);
                }
            }

            // /Desktop처럼 루트 기준 일반 폴더명인 경우
            if (expandedPath.match(/^\/[A-Za-z]+$/)) {
                const folderName = expandedPath.substring(1);
                const homeDir = os.homedir();
                const userFolder = path.join(homeDir, folderName);

                // 사용자 홈에 해당 폴더가 있는지 확인
                if (fs.existsSync(userFolder) && fs.lstatSync(userFolder).isDirectory()) {
                    return userFolder;
                }
            }

            return expandedPath;
        }

    // startRecording 메서드만 수정
    async startRecording(videoPort, audioPort, videoRtpParameters, audioRtpParameters) {
        try {
            // Spring Boot에 녹화 시작 알림 (확장된 경로 전달)
            const response = await axios.post(`${this.springBootUrl}/api/recordings/start`, {
                roomId: this.roomId,
                workspaceId: this.workspaceId,
                recorderId: this.recorderId,
                customPath: this.recordingPath  // ⭐ 확장된 경로 전달
            });

            console.log('Spring Boot 응답:', response.data);

            this.recordingId = response.data.recordingId;

            // 파일 경로 설정
            const fileName = path.basename(response.data.filePath);

            // ⭐ 사용자 지정 경로 사용
            const localDir = path.join(this.recordingPath, this.workspaceId, this.roomId);
            this.filePath = path.join(localDir, fileName);

            console.log('녹화 파일 경로:', this.filePath);

            this.videoPort = videoPort;
            this.audioPort = audioPort;

            // 디렉토리 생성
            if (!fs.existsSync(localDir)) {
                fs.mkdirSync(localDir, { recursive: true });
            }

            // GStreamer 녹화 준비
            console.log('GStreamer 녹화 준비 중...');

            // SSRC 정보 추출 - 변수명 수정!
            const videoSsrc = videoRtpParameters?.encodings?.[0]?.ssrc;
            const audioSsrc = audioRtpParameters?.encodings?.[0]?.ssrc;

            console.log('RTP 파라미터 정보:', {
                video: {
                    ssrc: videoSsrc,
                    payloadType: videoRtpParameters?.codecs?.[0]?.payloadType,
                    clockRate: videoRtpParameters?.codecs?.[0]?.clockRate
                },
                audio: {
                    ssrc: audioSsrc,
                    payloadType: audioRtpParameters?.codecs?.[0]?.payloadType,
                    clockRate: audioRtpParameters?.codecs?.[0]?.clockRate
                }
            });

            // ⭐ GStreamer 파이프라인 구성 - 변수명 수정!
            const videoCaps = `application/x-rtp,media=video,encoding-name=VP8,payload=${videoRtpParameters?.codecs?.[0]?.payloadType || 101},clock-rate=90000${videoSsrc ? `,ssrc=${videoSsrc}` : ''}`;
            const audioCaps = `application/x-rtp,media=audio,encoding-name=OPUS,payload=${audioRtpParameters?.codecs?.[0]?.payloadType || 100},clock-rate=48000${audioSsrc ? `,ssrc=${audioSsrc}` : ''}`;

            console.log('비디오 Caps:', videoCaps);
            console.log('오디오 Caps:', audioCaps);

            // ⭐ 개선된 파이프라인 with 디버그 옵션
            const gstCommand = 'gst-launch-1.0';
            const gstArgs = [
                '-v', // verbose 모드 추가
                '-e',
                // 비디오 파이프라인
                'udpsrc', `port=${this.videoPort}`, `caps=${videoCaps}`, '!',
                'rtpvp8depay', '!',
                'vp8dec', '!',
                'videoconvert', '!',
                'vp8enc', 'deadline=1', 'cpu-used=4', 'threads=4', '!',
                'queue', '!',
                'webmmux', 'name=mux', '!',
                'filesink', `location=${this.filePath}`,

                // 오디오 파이프라인
                'udpsrc', `port=${this.audioPort}`, `caps=${audioCaps}`, '!',
                'rtpopusdepay', '!',
                'opusdec', '!',
                'audioconvert', '!',
                'audioresample', '!',
                'opusenc', '!',
                'queue', '!',
                'mux.audio_0'
            ];

            console.log('GStreamer 명령어:', gstCommand, gstArgs.join(' '));

            // GStreamer 프로세스 시작
            this.gstreamerProcess = spawn(gstCommand, gstArgs);

            // 표준 출력 처리
            this.gstreamerProcess.stdout.on('data', (data) => {
                const message = data.toString();
                console.log(`GStreamer 출력: ${message}`);

                // UDP 소스가 데이터를 받고 있는지 확인
                if (message.includes('caps =')) {
                    console.log('📡 GStreamer가 RTP 스트림을 감지했습니다!');
                }
            });

            // 표준 오류 처리
            this.gstreamerProcess.stderr.on('data', (data) => {
                const message = data.toString();

                // 정보성 메시지와 실제 오류 구분
                if (message.includes('ERROR') || message.includes('CRITICAL')) {
                    console.error('❌ GStreamer 오류:', message);
                } else if (message.includes('WARNING')) {
                    console.warn('⚠️ GStreamer 경고:', message);
                } else {
                    console.log('GStreamer 메시지:', message);
                }
            });

            // 프로세스 오류 처리
            this.gstreamerProcess.on('error', (error) => {
                console.error('GStreamer 프로세스 오류:', error);
                this.handleRecordingError(error.message);
            });

            // 프로세스 종료 처리
            this.gstreamerProcess.on('close', (code) => {
                console.log(`GStreamer 프로세스 종료 (코드: ${code})`);
                this.isRecording = false;

                // 파일 존재 여부 확인
                if (fs.existsSync(this.filePath)) {
                    const stats = fs.statSync(this.filePath);
                    console.log(`녹화 파일 크기: ${stats.size} bytes`);
                } else {
                    console.error('녹화 파일이 생성되지 않았습니다!');
                }
            });

            // 프로세스 시작 확인
            await new Promise((resolve, reject) => {
                setTimeout(() => {
                    if (this.gstreamerProcess && !this.gstreamerProcess.killed) {
                        this.isRecording = true;
                        console.log('✅ GStreamer 녹화 시작됨');
                        resolve();
                    } else {
                        reject(new Error('GStreamer 프로세스 시작 실패'));
                    }
                }, 1000);
            });

            return {
                recordingId: this.recordingId,
                filePath: this.filePath
            };

        } catch (error) {
            console.error('녹화 시작 실패:', error);

            // 프로세스가 실행 중이면 종료
            if (this.gstreamerProcess && !this.gstreamerProcess.killed) {
                this.gstreamerProcess.kill('SIGKILL');
            }

            throw error;
        }
    }

    // GStreamer는 SDP 파일이 필요 없으므로 이 메서드는 더 이상 사용하지 않음
    createDetailedSDP(videoRtpParameters, audioRtpParameters) {
        // GStreamer는 caps 문자열로 RTP 파라미터를 직접 지정하므로 SDP 파일이 불필요
        console.log('GStreamer 방식에서는 SDP 파일을 사용하지 않습니다.');
        return null;
    }

   async stopRecording() {
           if (!this.isRecording || !this.gstreamerProcess) {
               throw new Error('녹화 중이 아닙니다');
           }

           try {
               // GStreamer에 EOS 신호 보내기
               this.gstreamerProcess.kill('SIGINT');

               // 프로세스 종료 대기 (최대 5초)
               await new Promise((resolve) => {
                   let timeout = setTimeout(() => {
                       // 5초 후에도 종료되지 않으면 강제 종료
                       this.gstreamerProcess.kill('SIGKILL');
                       resolve();
                   }, 5000);

                   this.gstreamerProcess.on('close', () => {
                       clearTimeout(timeout);
                       resolve();
                   });
               });

               // Spring Boot에 녹화 종료 알림
               const response = await axios.put(
                   `${this.springBootUrl}/api/recordings/${this.recordingId}/stop`,
                   {
                       fileSize: this.getFileSize(),
                       duration: null  // TODO: 실제 녹화 시간 계산
                   }
               );

               console.log('Spring Boot 녹화 종료 응답:', response.data);

               this.isRecording = false;
               this.gstreamerProcess = null;

               return {
                   recordingId: this.recordingId,
                   filePath: this.filePath,
                   fileSize: this.getFileSize()
               };

           } catch (error) {
               console.error('녹화 종료 실패:', error);
               throw error;
           }
       }

       getFileSize() {
           try {
               if (fs.existsSync(this.filePath)) {
                   const stats = fs.statSync(this.filePath);
                   return stats.size;
               }
           } catch (error) {
               console.error('파일 크기 확인 실패:', error);
           }
           return null;
       }

    /**
     * 녹화 에러 처리
     */
    async handleRecordingError(errorMessage) {
        this.isRecording = false;

        try {
            await axios.put(`${this.springBootUrl}/api/recordings/${this.recordingId}/fail`, {
                reason: errorMessage
            });
        } catch (error) {
            console.error('녹화 실패 처리 중 오류:', error);
        }
    }
}

module.exports = Recorder;