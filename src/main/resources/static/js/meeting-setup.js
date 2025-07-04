/**
 * 회의 준비 페이지 JavaScript - 수정된 버전
 * 카메라와 마이크를 독립적으로 제어할 수 있도록 개선
 */
class MeetingSetup {
  constructor() {
    this.localStream = null;
    this.videoTrack = null;
    this.audioTrack = null;
    this.audioContext = null;
    this.analyser = null;
    this.microphone = null;

    // 장치 상태 추적
    this.isCameraOn = false;
    this.isMicOn = false;

    this.initializeElements();
    this.initializeEventListeners();
    this.checkDevicePermissions();
  }

  /**
   * DOM 요소 초기화
   */
  initializeElements() {
    // 비디오 요소
    this.videoElement = document.getElementById('localVideo');
    this.videoPlaceholder = document.getElementById('videoPlaceholder');

    // 컨트롤 버튼
    this.cameraToggle = document.getElementById('cameraToggle');
    this.micToggle = document.getElementById('micToggle');

    // 장치 선택
    this.cameraSelect = document.getElementById('cameraSelect');
    this.micSelect = document.getElementById('micSelect');
    this.speakerSelect = document.getElementById('speakerSelect');

    // 볼륨 미터
    this.volumeBar = document.getElementById('volumeBar');

    // 폼 요소
    this.meetingForm = document.getElementById('meetingForm');

    // 기타
    this.loadingOverlay = document.getElementById('loadingOverlay');
  }

  /**
   * 이벤트 리스너 설정
   */
  initializeEventListeners() {
    // 미디어 컨트롤
    if (this.cameraToggle) {
      this.cameraToggle.addEventListener('click', () => this.toggleCamera());
    }
    if (this.micToggle) {
      this.micToggle.addEventListener('click', () => this.toggleMicrophone());
    }

    // 장치 변경
    if (this.cameraSelect) {
      this.cameraSelect.addEventListener('change', () => this.changeCamera());
    }
    if (this.micSelect) {
      this.micSelect.addEventListener('change', () => this.changeMicrophone());
    }
    if (this.speakerSelect) {
      this.speakerSelect.addEventListener('change', () => this.changeSpeaker());
    }

    // 테스트 버튼
    const testSpeakerBtn = document.getElementById('testSpeaker');
    if (testSpeakerBtn) {
      testSpeakerBtn.addEventListener('click', () => this.testSpeaker());
    }

    // 폼 제출
    if (this.meetingForm) {
      this.meetingForm.addEventListener('submit', (e) => this.handleSubmit(e));
    }
  }

  /**
   * 장치 권한 확인
   */
  async checkDevicePermissions() {
    try {
      // 권한 요청 (짧게만)
      const stream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true
      });

      // 즉시 정지
      stream.getTracks().forEach(track => track.stop());

      // 장치 목록 로드
      await this.loadDevices();
    } catch (error) {
      console.log('미디어 권한 대기 중');
    }
  }

  /**
   * 스트림 업데이트 - 트랙들을 하나의 스트림으로 합침
   */
  updateStream() {
    // 기존 스트림이 있으면 정리
    if (this.localStream) {
      // 기존 트랙들 제거
      this.localStream.getTracks().forEach(track => {
        this.localStream.removeTrack(track);
      });
    } else {
      // 새 스트림 생성
      this.localStream = new MediaStream();
    }

    // 활성 트랙들 추가
    if (this.videoTrack) {
      this.localStream.addTrack(this.videoTrack);
    }
    if (this.audioTrack) {
      this.localStream.addTrack(this.audioTrack);
    }

    // 비디오 요소에 스트림 설정
    if (this.videoTrack) {
      this.videoElement.srcObject = this.localStream;
      this.videoElement.style.display = 'block';
      this.videoPlaceholder.style.display = 'none';
    } else {
      this.videoElement.srcObject = null;
      this.videoElement.style.display = 'none';
      this.videoPlaceholder.style.display = 'flex';
    }
  }

  /**
   * 카메라 토글
   */
  async toggleCamera() {
    if (!this.isCameraOn) {
      await this.startCamera();
    } else {
      this.stopCamera();
    }
  }

  /**
   * 카메라만 시작
   */
  async startCamera() {
    try {
      const constraints = {
        video: {
          deviceId: this.cameraSelect.value ? { exact: this.cameraSelect.value } : undefined
        }
      };

      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      this.videoTrack = stream.getVideoTracks()[0];

      this.isCameraOn = true;
      this.updateStream();

      // 버튼 상태 업데이트
      this.cameraToggle.classList.add('active');

      // 버튼 텍스트 변경
      this.cameraToggle.querySelector('span').textContent = '카메라 끄기';

    } catch (error) {
      console.error('카메라 시작 실패:', error);
      alert('카메라를 시작할 수 없습니다. 권한을 확인해주세요.');
    }
  }

  /**
   * 카메라만 정지
   */
  stopCamera() {
    if (this.videoTrack) {
      this.videoTrack.stop();
      this.videoTrack = null;
    }

    this.isCameraOn = false;
    this.updateStream();

    // 버튼 상태 업데이트
    this.cameraToggle.classList.remove('active');

    // 버튼 텍스트 변경
    this.cameraToggle.querySelector('span').textContent = '카메라 켜기';
  }

  /**
   * 마이크 토글
   */
  async toggleMicrophone() {
    if (!this.isMicOn) {
      await this.startMicrophone();
    } else {
      this.stopMicrophone();
    }
  }

  /**
   * 마이크만 시작
   */
  async startMicrophone() {
    try {
      const constraints = {
        audio: {
          deviceId: this.micSelect.value ? { exact: this.micSelect.value } : undefined
        }
      };

      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      this.audioTrack = stream.getAudioTracks()[0];

      this.isMicOn = true;
      this.updateStream();

      // 버튼 상태 업데이트
      this.micToggle.classList.add('active');

      // 버튼 텍스트 변경
      this.micToggle.querySelector('span').textContent = '마이크 끄기';

      // 오디오 레벨 모니터링 시작
      this.startVolumeMonitor();

    } catch (error) {
      console.error('마이크 시작 실패:', error);
      alert('마이크를 시작할 수 없습니다. 권한을 확인해주세요.');
    }
  }

  /**
   * 마이크만 정지
   */
  stopMicrophone() {
    if (this.audioTrack) {
      this.audioTrack.stop();
      this.audioTrack = null;
    }

    this.isMicOn = false;
    this.updateStream();

    // 버튼 상태 업데이트
    this.micToggle.classList.remove('active');

    // 버튼 텍스트 변경
    this.micToggle.querySelector('span').textContent = '마이크 켜기';

    // 오디오 레벨 모니터링 중지
    this.stopVolumeMonitor();
  }

  /**
   * 오디오 레벨 모니터링 시작
   */
  startVolumeMonitor() {
    if (!this.localStream || !this.audioTrack) return;

    if (!this.audioContext) {
      this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
    }

    this.analyser = this.audioContext.createAnalyser();
    this.microphone = this.audioContext.createMediaStreamSource(this.localStream);
    this.microphone.connect(this.analyser);

    this.analyser.fftSize = 256;
    const bufferLength = this.analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    const updateVolume = () => {
      if (!this.analyser || !this.isMicOn) return;

      this.analyser.getByteFrequencyData(dataArray);
      const average = dataArray.reduce((a, b) => a + b) / bufferLength;
      const percentage = Math.min(100, (average / 255) * 200);

      if (this.volumeBar) {
        this.volumeBar.style.width = `${percentage}%`;
      }

      requestAnimationFrame(updateVolume);
    };

    updateVolume();
  }

  /**
   * 오디오 레벨 모니터링 중지
   */
  stopVolumeMonitor() {
    if (this.microphone) {
      this.microphone.disconnect();
      this.microphone = null;
    }
    if (this.analyser) {
      this.analyser = null;
    }
    if (this.volumeBar) {
      this.volumeBar.style.width = '0%';
    }
  }

  /**
   * 카메라 변경
   */
  async changeCamera() {
    if (this.isCameraOn) {
      this.stopCamera();
      await this.startCamera();
    }
  }

  /**
   * 마이크 변경
   */
  async changeMicrophone() {
    if (this.isMicOn) {
      this.stopMicrophone();
      await this.startMicrophone();
    }
  }

  /**
   * 스피커 변경
   */
  async changeSpeaker() {
    if (this.videoElement && this.speakerSelect.value) {
      try {
        await this.videoElement.setSinkId(this.speakerSelect.value);
      } catch (error) {
        console.error('스피커 변경 실패:', error);
      }
    }
  }

  /**
   * 스피커 테스트
   */
  testSpeaker() {
    const audio = new Audio('/sounds/test-sound.mp3');
    if (this.speakerSelect.value) {
      audio.setSinkId(this.speakerSelect.value);
    }
    audio.play();
  }

  /**
   * 장치 목록 로드
   */
  async loadDevices() {
    try {
      const devices = await navigator.mediaDevices.enumerateDevices();

      // 카메라 목록
      const videoDevices = devices.filter(device => device.kind === 'videoinput');
      this.cameraSelect.innerHTML = '<option value="">카메라를 선택하세요</option>';
      videoDevices.forEach(device => {
        const option = document.createElement('option');
        option.value = device.deviceId;
        option.text = device.label || `카메라 ${this.cameraSelect.options.length}`;
        this.cameraSelect.appendChild(option);
      });

      // 마이크 목록
      const audioDevices = devices.filter(device => device.kind === 'audioinput');
      this.micSelect.innerHTML = '<option value="">마이크를 선택하세요</option>';
      audioDevices.forEach(device => {
        const option = document.createElement('option');
        option.value = device.deviceId;
        option.text = device.label || `마이크 ${this.micSelect.options.length}`;
        this.micSelect.appendChild(option);
      });

      // 스피커 목록
      const audioOutputDevices = devices.filter(device => device.kind === 'audiooutput');
      this.speakerSelect.innerHTML = '<option value="">스피커를 선택하세요</option>';
      audioOutputDevices.forEach(device => {
        const option = document.createElement('option');
        option.value = device.deviceId;
        option.text = device.label || `스피커 ${this.speakerSelect.options.length}`;
        this.speakerSelect.appendChild(option);
      });

    } catch (error) {
      console.error('장치 목록 로드 실패:', error);
    }
  }

  /**
   * 폼 제출 처리
   */
  async handleSubmit(e) {
    e.preventDefault();

    // 로딩 표시
    if (this.loadingOverlay) {
      this.loadingOverlay.style.display = 'flex';
    }

    try {
      // 폼 데이터 수집
      const formData = new FormData(this.meetingForm);

      // 선택된 멤버 수집
      const invitedMembers = [];
      document.querySelectorAll('input[name="invitedMembers"]:checked').forEach(checkbox => {
        invitedMembers.push(checkbox.value);
      });

      // 요청 데이터 구성
      const requestData = {
        title: formData.get('meetingTitle'),
        description: document.getElementById('meetingDesc').value,
        workspaceCd: workspaceCd,
        autoRecord: formData.get('autoRecord') === 'on',
        muteOnJoin: formData.get('muteOnJoin') === 'on',
        waitingRoom: formData.get('waitingRoom') === 'on',
        videoQuality: formData.get('videoQuality'),
        invitedMembers: invitedMembers
      };

      // 회의 생성 API 호출
      const response = await fetch('/meeting/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      });

      if (!response.ok) {
        throw new Error('회의 생성 실패');
      }

      const result = await response.json();

      // 스트림 정리
      this.cleanup();

      // 회의실로 이동
      window.location.href = `${mediaServerUrl}/ocean-video-chat-complete.html?${new URLSearchParams({
        roomId: result.roomId,
        workspaceId: workspaceCd,
        peerId: currentUserId,
        displayName: result.displayName || '사용자',
        meetingType: 'sketch',
        autoRecord: requestData.autoRecord,
        muteOnJoin: requestData.muteOnJoin,
        videoQuality: requestData.videoQuality
      })}`;

    } catch (error) {
      console.error('회의 생성 실패:', error);
      alert('회의를 시작할 수 없습니다. 다시 시도해주세요.');

      if (this.loadingOverlay) {
        this.loadingOverlay.style.display = 'none';
      }
    }
  }

  /**
   * 리소스 정리
   */
  cleanup() {
    // 카메라 정지
    if (this.videoTrack) {
      this.videoTrack.stop();
      this.videoTrack = null;
    }

    // 마이크 정지
    if (this.audioTrack) {
      this.audioTrack.stop();
      this.audioTrack = null;
    }

    // 스트림 정리
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop());
      this.localStream = null;
    }

    // 오디오 컨텍스트 정리
    this.stopVolumeMonitor();
    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }
  }
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', () => {
  window.meetingSetup = new MeetingSetup();
});

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', () => {
  if (window.meetingSetup) {
    window.meetingSetup.cleanup();
  }
});