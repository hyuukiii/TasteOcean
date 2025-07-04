// meeting-setup.js

/**
 * 회의 준비 페이지 JavaScript
 */

class MeetingSetup {
  constructor() {
    this.localStream = null;
    this.audioContext = null;
    this.analyser = null;
    this.microphone = null;

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
      // 권한 요청
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
   * 카메라 토글
   */
  async toggleCamera() {
    if (!this.localStream || !this.localStream.getVideoTracks().length) {
      await this.startCamera();
    } else {
      this.stopCamera();
    }
  }

  /**
   * 카메라 시작
   */
  async startCamera() {
    try {
      const constraints = {
        video: {
          deviceId: this.cameraSelect.value ? { exact: this.cameraSelect.value } : undefined
        },
        audio: {
          deviceId: this.micSelect.value ? { exact: this.micSelect.value } : undefined
        }
      };

      this.localStream = await navigator.mediaDevices.getUserMedia(constraints);
      this.videoElement.srcObject = this.localStream;
      this.videoElement.style.display = 'block';
      this.videoPlaceholder.style.display = 'none';

      // 버튼 상태 업데이트
      this.cameraToggle.classList.add('active');
      this.micToggle.classList.add('active');

      // 오디오 레벨 모니터링 시작
      this.startVolumeMonitor();

    } catch (error) {
      console.error('카메라 시작 실패:', error);
      alert('카메라를 시작할 수 없습니다. 권한을 확인해주세요.');
    }
  }

  /**
   * 카메라 정지
   */
  stopCamera() {
    if (this.localStream) {
      this.localStream.getVideoTracks().forEach(track => track.stop());
      this.videoElement.style.display = 'none';
      this.videoPlaceholder.style.display = 'flex';
      this.cameraToggle.classList.remove('active');
    }
  }

  /**
   * 마이크 토글
   */
  toggleMicrophone() {
    if (this.localStream) {
      const audioTrack = this.localStream.getAudioTracks()[0];
      if (audioTrack) {
        audioTrack.enabled = !audioTrack.enabled;
        this.micToggle.classList.toggle('active', audioTrack.enabled);
      }
    }
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
      alert('회의를 시작할 수 없습니다. 다시 시도해주세요.');

      this.loadingOverlay.style.display = 'none';
    }
  }

  /**
   * 오디오 레벨 모니터링 시작
   */
  startVolumeMonitor() {
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
      if (!this.analyser) return;

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
   * 카메라 변경
   */
  async changeCamera() {
    if (this.localStream && this.localStream.getVideoTracks().length > 0) {
      this.stopCamera();
      await this.startCamera();
    }
  }

  /**
   * 마이크 변경
   */
  async changeMicrophone() {
    if (this.localStream && this.localStream.getAudioTracks().length > 0) {
      this.stopCamera();
      await this.startCamera();
    }
  }

  /**
   * 스피커 변경
   */
  async changeSpeaker() {
    if (this.videoElement && this.videoElement.setSinkId) {
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
    if (this.speakerSelect.value && audio.setSinkId) {
      audio.setSinkId(this.speakerSelect.value);
    }
    audio.play();
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

// ===== 클래스 밖에 모달 관련 전역 함수들 정의 =====

let currentMemberId = null;

/**
 * 멤버 정보 모달 표시
 */
function showMemberModal(userId, name, dept, position, email, phone, statusMsg, userImg) {
  currentMemberId = userId;

  // 모달 요소 가져오기
  const modal = document.getElementById('memberModal');
  const profileImg = modal.querySelector('.profile-image-large img');
  const profileInitial = modal.querySelector('.profile-initial-large');

  // 프로필 이미지 처리
  if (userImg && userImg !== 'null' && userImg !== '') {
    profileImg.src = userImg;
    profileImg.style.display = 'block';
    profileInitial.style.display = 'none';
  } else {
    profileImg.style.display = 'none';
    profileInitial.style.display = 'flex';
    profileInitial.textContent = name ? name.substring(0, 1) : '?';

    // 이름에 따른 색상 설정
    const colors = ['#4a9eff', '#ff6b6b', '#51cf66', '#ff922b', '#845ef7'];
    const colorIndex = name ? name.charCodeAt(0) % colors.length : 0;
    profileInitial.style.backgroundColor = colors[colorIndex];
  }

  // 정보 업데이트
  document.getElementById('modalMemberName').textContent = name || '미설정';
  document.getElementById('modalMemberPosition').textContent = position || '';
  document.getElementById('modalMemberDept').textContent = dept || '-';
  document.getElementById('modalMemberEmail').textContent = email || '-';
  document.getElementById('modalMemberPhone').textContent = phone || '-';
  document.getElementById('modalMemberStatus').textContent = statusMsg || '-';

  // 현재 사용자인 경우 초대 버튼 숨기기
  const inviteBtn = modal.querySelector('.btn-primary');
  if (userId === currentUserId) {
    inviteBtn.style.display = 'none';
  } else {
    inviteBtn.style.display = 'flex';
  }

  // 모달 표시
  modal.classList.add('show');
  document.body.style.overflow = 'hidden'; // 배경 스크롤 방지
}

/**
 * 멤버 정보 모달 닫기
 */
function closeMemberModal() {
  const modal = document.getElementById('memberModal');
  modal.classList.remove('show');
  document.body.style.overflow = ''; // 스크롤 복원
  currentMemberId = null;
}

/**
 * 멤버 초대 (체크박스 체크)
 */
function inviteMember() {
  if (!currentMemberId) return;

  // 해당 멤버의 체크박스 찾기
  const checkbox = document.querySelector(`input[name="invitedMembers"][value="${currentMemberId}"]`);
  if (checkbox && !checkbox.disabled) {
    checkbox.checked = true;

    // 시각적 피드백
    const memberItem = checkbox.closest('.member-item');
    memberItem.style.backgroundColor = 'rgba(74, 158, 255, 0.1)';
    setTimeout(() => {
      memberItem.style.backgroundColor = '';
    }, 300);
  }

  closeMemberModal();
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', () => {
  window.meetingSetup = new MeetingSetup();

  // 모달 외부 클릭 시 닫기
  const modalOverlay = document.getElementById('memberModal');
  if (modalOverlay) {
    modalOverlay.addEventListener('click', function(e) {
      if (e.target === modalOverlay) {
        closeMemberModal();
      }
    });
  }

  // ESC 키로 모달 닫기
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' && modalOverlay && modalOverlay.classList.contains('show')) {
      closeMemberModal();
    }
  });
});

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', () => {
  if (window.meetingSetup) {
    window.meetingSetup.cleanup();
  }
});