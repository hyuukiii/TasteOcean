document.addEventListener('DOMContentLoaded', function () {
  const openBtn = document.querySelector('.invite-member');
  const modal = document.getElementById('inviteModal');
  const overlay = document.getElementById('inviteOverlay');
  const closeBtn = modal?.querySelector('.close-btn');

  const emailInput = document.getElementById("inviteEmail");
  const emailError = document.getElementById("emailError");
  const emailSuccess = document.getElementById("emailSuccess");
  const copySuccess = document.getElementById("copySuccess");
  const sendBtn = document.querySelector(".send-btn");
  const copyBtn = document.querySelector(".copy-btn");
  const inviteCode = document.getElementById("inviteCode");

  // 모달 열기
  if (openBtn) {
    openBtn.addEventListener('click', () => {
      modal.style.display = 'block';
      overlay.style.display = 'block';

      // 리셋
      emailInput.value = '';
      emailError.style.display = 'none';
      emailSuccess.style.display = 'none';
      copySuccess.style.display = 'none';
    });
  }

  // 모달 닫기
  const closeModal = () => {
    modal.style.display = 'none';
    overlay.style.display = 'none';

    emailInput.value = '';
    emailError.style.display = 'none';
    emailSuccess.style.display = 'none';
    copySuccess.style.display = 'none';
  };

  if (closeBtn) closeBtn.addEventListener('click', closeModal);
  if (overlay) overlay.addEventListener('click', closeModal);

  // 이메일 전송 버튼
  if (sendBtn) {
    sendBtn.addEventListener('click', () => {
      const email = emailInput.value.trim();
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      const code = inviteCode.textContent;

      emailError.style.display = 'none';
      emailSuccess.style.display = 'none';

      if (email === "" || !emailRegex.test(email)) {
        emailError.style.display = 'block';
        return;
      }

      // 실제 이메일 전송 요청
      fetch('/api/workspaces/invite-email', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email: email, inviteCode: code })
      })
      .then(res => {
        if (!res.ok) throw new Error("이메일 전송 실패");
        return res.text();
      })
      .then(msg => {
        emailSuccess.style.display = 'block';
      })
      .catch(err => {
        emailError.textContent = "전송 실패: " + err.message;
        emailError.style.display = 'block';
      });
    });
  }

  // 초대 코드 복사 버튼
  if (copyBtn) {
    copyBtn.addEventListener('click', () => {
      navigator.clipboard.writeText(inviteCode.textContent)
        .then(() => {
          copySuccess.style.display = 'block';
        })
        .catch(() => {
          alert("복사 실패");
        });
    });
  }
});
