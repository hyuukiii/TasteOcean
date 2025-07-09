document.addEventListener('DOMContentLoaded', function() {
    // 스와이퍼 초기화
    const swiper = new Swiper('.swiper-container', {
        allowTouchMove: false,
        pagination: {
            el: '.swiper-pagination',
            clickable: true,
        },
        navigation: {
            nextEl: '.btn-next',
            prevEl: '.btn-prev',
        },
    });

    const workspaceForm = document.getElementById('workspaceForm');
    const nameInput = document.getElementById('workspaceNm');

    // 다음 버튼 클릭 이벤트
    document.querySelector('.btn-next').addEventListener('click', function() {
        validateFirstSlide();
    });

    // 이전 버튼 클릭 이벤트
    document.querySelectorAll('.btn-prev').forEach(btn => {
        btn.addEventListener('click', function() {
            swiper.slidePrev();
        });
    });

    function validateFirstSlide() {
            // reportValidity()는 유효하면 true를 반환하고, 유효하지 않으면 false를 반환하며 사용자에게 유효성 메시지를 표시합니다.
            if (nameInput.reportValidity()) {
                swiper.slideNext(); // 유효할 때만 다음 슬라이드로 이동합니다.
            }
        }

    // 폼 제출 이벤트 처리
    document.getElementById('workspaceForm').addEventListener('submit', function(e) {
        e.preventDefault(); // 기본 폼 제출 방지


        // 워크스페이스 이름이 유효한지 먼저 확인합니다.
        if (!nameInput.checkValidity()) {
            // 유효하지 않다면, 첫 번째 슬라이드로 이동합니다.
            swiper.slideTo(0);
            // 이름 입력창에 포커스를 주고, 브라우저의 기본 유효성 검사 메시지를 표시합니다.
            nameInput.reportValidity();
            return; // 생성 로직을 중단합니다.
        }

        // 모든 유효성 검사를 통과했을 때만 워크스페이스 생성 함수를 호출합니다.
        createWorkspace();
    });

   // 워크스페이스 생성 함수
   async function createWorkspace() {
       // 로딩 표시
       const submitBtn = document.querySelector('.btn-create');
       const originalText = submitBtn.textContent;
       submitBtn.disabled = true;
       submitBtn.textContent = '생성 중...';

       try {
           // FormData 객체 생성
           const formData = new FormData();

           // 기본 필드 추가
           formData.append('workspaceNm', document.getElementById('workspaceNm').value);

           const endDate = document.getElementById('endDate').value;
           if (endDate) {
               formData.append('endDate', endDate);
           }

           const description = document.getElementById('description').value;
           if (description) {
               formData.append('description', description);
           }

           // 부서 정보 추가
           document.querySelectorAll('input[name="departments"]').forEach(input => {
               if (input.value.trim()) {
                   formData.append('departments', input.value.trim());
               }
           });

           // 이미지 파일 추가
           const fileInput = document.getElementById('upload');
           if (fileInput.files.length > 0) {
               formData.append('workspaceImageFile', fileInput.files[0]);
           }

           // 액세스 토큰 가져오기
           const accessToken = localStorage.getItem('accessToken');
           if (!accessToken) {
               alert('로그인이 필요합니다.');
               window.location.href = '/login';
               return;
           }

           // API 호출 - Content-Type 헤더를 설정하지 않음 (브라우저가 자동으로 설정)
           const response = await fetch('/api/workspaces', {
               method: 'POST',
               headers: {
                   'Authorization': 'Bearer ' + accessToken
                   // Content-Type은 설정하지 않음! FormData 사용 시 브라우저가 자동으로 설정
               },
               body: formData
           });

           // 응답 처리
           const contentType = response.headers.get('content-type');
           let responseData;

           if (contentType && contentType.includes('application/json')) {
               responseData = await response.json();
           } else {
               // JSON이 아닌 경우 텍스트로 읽기
               const text = await response.text();
               console.error('Non-JSON response:', text);
               throw new Error('서버 응답 형식 오류');
           }

           if (!response.ok) {
               throw new Error(responseData.message || '워크스페이스 생성 실패');
           }

           // 성공 메시지
           alert(`워크스페이스가 생성되었습니다!\n초대 코드: ${responseData.inviteCd}`);

           // 워크스페이스 목록으로 이동
           window.location.href = `/workspace`;
           //window.location.href = `/wsmain?workspaceCd=${responseData.workspaceCd}`;

       } catch (error) {
           console.error('Error:', error);
           alert(error.message || '워크스페이스 생성 중 오류가 발생했습니다.');
       } finally {
           // 버튼 상태 복원
           submitBtn.disabled = false;
           submitBtn.textContent = originalText;
       }
   }

    // 이미지 업로드 처리
    document.getElementById('upload').addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
            // 이미지 미리보기
            const reader = new FileReader();
            reader.onload = function(e) {
                document.getElementById('preview').src = e.target.result;
            };
            reader.readAsDataURL(file);
        }
    });
});

// 부서 추가/삭제 함수
function addDepartment() {
    const departmentSection = document.getElementById('department-section');
    const newDepartment = document.createElement('div');
    newDepartment.className = 'department-input-group';
    newDepartment.innerHTML = `
        <input type="text" name="departments" placeholder="부서명" required>
        <button type="button" class="btn-remove-department" onclick="removeDepartment(this)">×</button>
    `;
    departmentSection.appendChild(newDepartment);
}

function removeDepartment(btn) {
    // 현재 화면에 있는 부서 입력란의 개수를 확인.
    const departmentCount = document.querySelectorAll('.department-input-group').length;

    // 부서가 1개 이하이면 경고창을 띄우고 함수를 종료.
    if (departmentCount <= 1) {
        alert('워크스페이스에는 최소 1개 이상의 부서가 필요합니다.');
        return;
    }

    // 부서가 2개 이상일 경우에만 삭제를 실행.
    btn.parentElement.remove();
}