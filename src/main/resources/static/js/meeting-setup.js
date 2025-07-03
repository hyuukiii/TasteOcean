// meeting-setup.js

/**
 * 회의 준비 페이지 JavaScript
 * Google JavaScript Style Guide 준수
 */

class MeetingSetup {
  constructor() {
    this.localStream = null;
    this.audioContext = null;
    this.analyser = null;
    this.microphone = null;
    this.invitedEmails = new Set();

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
    this.emailInput = document.getElementById('inviteEmail');
    this.emailList = document.getElementById('emailList');

    // 기타
    this.loadingOverlay = document.getElementById('loadingOverlay');
  }

  /**
   * 이벤트 리스너 설정
   */
  initializeEventListeners() {
    // 미디어 컨트롤
    this.cameraToggle.addEventListener('click', () => this.toggleCamera());
    this.micToggle.addEventListener('click', () => this.toggleMicrophone());

    // 장치 변경
    this.cameraSelect.addEventListener('change', () => this.changeCamera());
    this.micSelect.addEventListener('change', () => this.changeMicrophone());

    // 스피커 테스트
    document.getElementById('testSpeaker').addEventListener('click', () => this.testSpeaker());

    // 이메일 추가
    document.getElementById('addEmail').addEventListener('click', () => this.addEmail());
    this.emailInput.addEventListener('keypress', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        this.addEmail();
      }
    });

    // 폼 제출
    this.meetingForm.addEventListener('submit', (e) => this.handleSubmit(e));
  }

  /**
   * 장치 권한 확인 및 목록 로드
   */
  async checkDevicePermissions() {
    try {
      // 임시 스트림으로 권한 요청
      const tempStream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true
      });

      // 즉시 정지
      tempStream.getTracks().forEach(track => track.stop());

      // 장치 목록 로드
      await this.loadDevices();
    } catch (error) {
      console.log('초기 권한 요청 스킵:', error.message);
      // 권한이 없어도 장치 목록은 로드 시도
      await this.loadDevices();
    }
  }

  /**
   * 미디어 장치 목록 로드
   */
  async loadDevices() {
    try {
      const devices = await navigator.mediaDevices.enumerateDevices();

      // 기존 옵션 제거
      this.clearSelectOptions(this.cameraSelect);
      this.clearSelectOptions(this.micSelect);
      this.clearSelectOptions(this.speakerSelect);

      // 장치별로 분류
      devices.forEach(device => {
        const option = document.createElement('option');
        option.value = device.deviceId;
        option.text = device.label || `${device.kind} ${device.deviceId.substr(0, 8)}`;

        if (device.kind === 'videoinput') {
          this.cameraSelect.appendChild(option);
        } else if (device.kind === 'audioinput') {
          this.micSelect.appendChild(option);
        } else if (device.kind === 'audiooutput') {
          this.speakerSelect.appendChild(option);
        }
      });

      // 기본 장치 선택
      if (this.localStream) {
        const videoTrack = this.localStream.getVideoTracks()[0];
        const audioTrack = this.localStream.getAudioTracks()[0];

        if (videoTrack) {
          this.cameraSelect.value = videoTrack.getSettings().deviceId || '';
        }
        if (audioTrack) {
          this.micSelect.value = audioTrack.getSettings().deviceId || '';
        }
      }
    } catch (error) {
      console.error('장치 목록 로드 실패:', error);
    }
  }

  /**
   * select 요소의 옵션 초기화
   */
  clearSelectOptions(selectElement) {
    while (selectElement.options.length > 1) {
      selectElement.remove(1);
    }
  }

  /**
   * 카메라 토글
   */
  async toggleCamera() {
    if (this.localStream && this.localStream.getVideoTracks().length > 0) {
      // 카메라 끄기
      this.localStream.getVideoTracks().forEach(track => {
        track.stop();
        this.localStream.removeTrack(track);
      });

      this.videoElement.style.display = 'none';
      this.videoPlaceholder.style.display = 'flex';
      this.cameraToggle.classList.remove('active');
      this.cameraToggle.querySelector('span').textContent = '카메라 켜기';
    } else {
      // 카메라 켜기
      try {
        const constraints = {
          video: {
            deviceId: this.cameraSelect.value ?
              { exact: this.cameraSelect.value } : undefined,
            width: { ideal: 1280 },
            height: { ideal: 720 }
          }
        };

        const videoStream = await navigator.mediaDevices.getUserMedia(constraints);

        if (!this.localStream) {
          this.localStream = new MediaStream();
        }

        videoStream.getVideoTracks().forEach(track => {
          this.localStream.addTrack(track);
        });

        this.videoElement.srcObject = this.localStream;
        this.videoElement.style.display = 'block';
        this.videoPlaceholder.style.display = 'none';
        this.cameraToggle.classList.add('active');
        this.cameraToggle.querySelector('span').textContent = '카메라 끄기';

        // 장치 목록 업데이트
        await this.loadDevices();
      } catch (error) {
        console.error('카메라 시작 실패:', error);
        alert('카메라를 시작할 수 없습니다. 권한을 확인해주세요.');
      }
    }
  }

  /**
   * 마이크 토글
   */
  async toggleMicrophone() {
    if (this.localStream && this.localStream.getAudioTracks().length > 0) {
      // 마이크 끄기
      this.localStream.getAudioTracks().forEach(track => {
        track.stop();
        this.localStream.removeTrack(track);
      });

      this.stopVolumeMonitor();
      this.micToggle.classList.remove('active');
      this.micToggle.querySelector('span').textContent = '마이크 켜기';
    } else {
      // 마이크 켜기
      try {
        const constraints = {
          audio: {
            deviceId: this.micSelect.value ?
              { exact: this.micSelect.value } : undefined,
            echoCancellation: true,
            noiseSuppression: true,
            autoGainControl: true
          }
        };

        const audioStream = await navigator.mediaDevices.getUserMedia(constraints);

        if (!this.localStream) {
          this.localStream = new MediaStream();
        }

        audioStream.getAudioTracks().forEach(track => {
          this.localStream.addTrack(track);
        });

        this.micToggle.classList.add('active');
        this.micToggle.querySelector('span').textContent = '마이크 끄기';

        // 볼륨 모니터링 시작
        this.startVolumeMonitor(audioStream);

        // 장치 목록 업데이트
        await this.loadDevices();
      } catch (error) {
        console.error('마이크 시작 실패:', error);
        alert('마이크를 시작할 수 없습니다. 권한을 확인해주세요.');
      }
    }
  }

  /**
   * 카메라 변경
   */
  async changeCamera() {
    if (this.localStream && this.localStream.getVideoTracks().length > 0) {
      // 기존 카메라 정지
      this.localStream.getVideoTracks().forEach(track => track.stop());

      // 새 카메라로 시작
      this.cameraToggle.classList.remove('active');
      await this.toggleCamera();
    }
  }

  /**
   * 마이크 변경
   */
  async changeMicrophone() {
    if (this.localStream && this.localStream.getAudioTracks().length > 0) {
      // 기존 마이크 정지
      this.localStream.getAudioTracks().forEach(track => track.stop());

      // 새 마이크로 시작
      this.micToggle.classList.remove('active');
      await this.toggleMicrophone();
    }
  }

  /**
   * 볼륨 모니터링 시작
   */
  startVolumeMonitor(stream) {
    this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
    this.analyser = this.audioContext.createAnalyser();
    this.microphone = this.audioContext.createMediaStreamSource(stream);

    this.analyser.smoothingTimeConstant = 0.8;
    this.analyser.fftSize = 1024;

    this.microphone.connect(this.analyser);

    const bufferLength = this.analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    const checkVolume = () => {
      this.analyser.getByteFrequencyData(dataArray);

      let sum = 0;
      for (let i = 0; i < bufferLength; i++) {
        sum += dataArray[i];
      }

      const average = sum / bufferLength;
      const percentage = Math.min(100, (average / 128) * 100);

      this.volumeBar.style.width = percentage + '%';

      if (this.audioContext && this.audioContext.state === 'running') {
        requestAnimationFrame(checkVolume);
      }
    };

    checkVolume();
  }

  /**
   * 볼륨 모니터링 중지
   */
  stopVolumeMonitor() {
    if (this.microphone) {
      this.microphone.disconnect();
      this.microphone = null;
    }

    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }

    this.volumeBar.style.width = '0%';
  }

  /**
   * 스피커 테스트
   */
  async testSpeaker() {
    try {
      const audio = new Audio('/sounds/test-sound.mp3');

      // 선택된 스피커로 출력 설정
      if (this.speakerSelect.value && audio.setSinkId) {
        await audio.setSinkId(this.speakerSelect.value);
      }

      audio.play();
    } catch (error) {
      console.error('스피커 테스트 실패:', error);

      // 대체 테스트 사운드
      const oscillator = new (window.AudioContext || window.webkitAudioContext)();
      const osc = oscillator.createOscillator();
      const gain = oscillator.createGain();

      osc.connect(gain);
      gain.connect(oscillator.destination);

      gain.gain.setValueAtTime(0.1, oscillator.currentTime);
      osc.frequency.setValueAtTime(440, oscillator.currentTime);

      osc.start();
      osc.stop(oscillator.currentTime + 0.5);
    }
  }

  /**
   * 이메일 추가
   */
  addEmail() {
    const email = this.emailInput.value.trim();

    if (!email) return;

    // 이메일 형식 검증
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      alert('올바른 이메일 형식이 아닙니다.');
      return;
    }

    // 중복 확인
    if (this.invitedEmails.has(email)) {
      alert('이미 추가된 이메일입니다.');
      return;
    }

    this.invitedEmails.add(email);
    this.renderEmailList();
    this.emailInput.value = '';
  }

  /**
   * 이메일 목록 렌더링
   */
  renderEmailList() {
    this.emailList.innerHTML = '';

    this.invitedEmails.forEach(email => {
      const tag = document.createElement('div');
      tag.className = 'email-tag';
      tag.innerHTML = `
        ${email}
        <span class="remove" data-email="${email}">×</span>
      `;

      tag.querySelector('.remove').addEventListener('click', () => {
        this.invitedEmails.delete(email);
        this.renderEmailList();
      });

      this.emailList.appendChild(tag);
    });
  }

  /**
   * 폼 제출 처리
   */
  async handleSubmit(e) {
    e.preventDefault();

    // 로딩 표시
    this.loadingOverlay.style.display = 'flex';

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
        duration: parseInt(formData.get('duration')),
        workspaceCd: workspaceCd,
        autoRecord: formData.get('autoRecord') === 'on',
        muteOnJoin: formData.get('muteOnJoin') === 'on',
        waitingRoom: formData.get('waitingRoom') === 'on',
        videoQuality: formData.get('videoQuality'),
        invitedMembers: invitedMembers,
        invitedEmails: Array.from(this.invitedEmails)
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
      if (this.localStream) {
        this.localStream.getTracks().forEach(track => track.stop());
      }

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

        // 더 구체적인 에러 메시지
      if (error.message === '회의 생성 실패') {
          alert('회의를 생성할 수 없습니다. 네트워크 연결을 확인해주세요.');
      } else {
        alert('회의를 시작할 수 없습니다. 다시 시도해주세요.');
      }

      this.loadingOverlay.style.display = 'none';
    }
  }

  /**
   * 정리 작업
   */
  cleanup() {
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop());
    }

    this.stopVolumeMonitor();
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