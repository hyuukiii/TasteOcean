(() => {
    let workspaceCd = null;
    let loggedInUserId = null; // 내 userId 저장

    function showProfileModel(userId) {
        if (!workspaceCd) {
            console.error("⛔workspaceCd가 설정되어 있지 않습니다.");
            return;
        }

        fetch(`/api/workspaces/${workspaceCd}/member/${userId}`)
            .then((response) => response.json())
            .then((data) => {
                console.log(data);

                const modal = document.getElementById("profileModal");

                const viewProfileImg = document.getElementById("viewProfileImg");
                viewProfileImg.src = data.userImg || "/images/default.png";

                document.getElementById("viewNickname").textContent = data.userNickname || "-";
                document.getElementById("viewPhone").textContent = data.phoneNum || "-";
                document.getElementById("viewPosition").textContent = data.position || "-";
                document.getElementById("viewEmail").textContent = data.email || "-";
                document.getElementById("viewDept").textContent = data.deptNm || "-";

                // 🔸 내 정보일 때만 편집 버튼 보이기
                const toggleBtn = document.getElementById("toggleEditBtn");
                if (loggedInUserId === data.userId) {
                    toggleBtn.style.display = "inline-block";
                } else {
                    toggleBtn.style.display = "none";
                }

                modal.style.display = "block";
                document.getElementById("profileModalOverlay").style.display = "block";
            });
    }

    function closeProfileModal() {
        document.getElementById("profileModal").style.display = "none";
        document.getElementById("profileModalOverlay").style.display = "none";
    }

    document.addEventListener("DOMContentLoaded", async () => {
        const closeBtn = document.getElementById("closeProfileModal");
        const overlay = document.getElementById("profileModalOverlay");

        if (closeBtn) closeBtn.addEventListener("click", closeProfileModal);
        if (overlay) overlay.addEventListener("click", (e) => {
            if (e.target === overlay) closeProfileModal();
        });

        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") closeProfileModal();
        });

        const rnbContainer = document.getElementById("rnbContainer");
        const inviteModalContainer = document.getElementById("inviteModalContainer");
        const profileModalContainer = document.getElementById("profileModalContainer");

        try {
            workspaceCd = rnbContainer?.dataset.workspaceCd;
            if (!workspaceCd) {
                console.error("workspaceCd 없음");
                return;
            }

            const rnbHtml = await fetch("/html/rnb.html").then(res => res.text());
            rnbContainer.innerHTML = rnbHtml;

            bindStatusChangeEvents();

            const modelHtml = await fetch("/html/invite-modal.html").then(res => res.text());
            inviteModalContainer.innerHTML = modelHtml;

            const profileModelHtml = await fetch("/html/profile-modal.html").then(res => res.text());
            profileModalContainer.innerHTML = profileModelHtml;

            document.getElementById("myInfoBtn").addEventListener("click", async () => {
                try {
                    const profileRes = await fetch(`/api/workspaces/${workspaceCd}/profile`);
                    if (!profileRes.ok) throw new Error("내 프로필 API 실패");

                    const myProfile = await profileRes.json();
                    loggedInUserId = myProfile.userId; // 내 userId 저장
                    localStorage.setItem("userId", myProfile.userId);
                    localStorage.setItem("workspaceCd", workspaceCd);

                    // 데이터 렌더링
                    document.getElementById("viewProfileImg").src = getImagePath(myProfile.userImg);
                    document.getElementById("viewNickname").textContent = myProfile.userNickname || "-";
                    document.getElementById("viewPhone").textContent = myProfile.phoneNum || "-";
                    document.getElementById("viewPosition").textContent = myProfile.position || "-";
                    document.getElementById("viewEmail").textContent = myProfile.email || "-";
                    document.getElementById("viewDept").textContent = myProfile.deptNm || "-";

                    document.getElementById("toggleEditBtn").style.display = "inline-block";

                    document.getElementById("profileModal").style.display = "block";
                    document.getElementById("profileModalOverlay").style.display = "block";
                } catch (e) {
                    console.error("내 정보 모달 로딩 실패:", e);
                }
            });

            const profileCloseBtn = document.getElementById("closeProfileModal");
            const profileOverlay = document.getElementById("profileModalOverlay");

            if (profileCloseBtn) {
                profileCloseBtn.addEventListener("click", closeProfileModal);
            }
            if (profileOverlay) {
                profileOverlay.addEventListener("click", (e) => {
                    if (e.target === profileOverlay) closeProfileModal();
                });
            }

            setTimeout(() => {
                const inviteBtn = document.querySelector(".invite-member");
                const modal = document.getElementById("inviteModal");
                const overlay = document.getElementById("inviteOverlay");

                const emailInput = document.getElementById("inviteEmail");
                const emailError = document.getElementById("emailError");
                const emailSuccess = document.getElementById("emailSuccess");
                const copySuccess = document.getElementById("copySuccess");

                inviteBtn.addEventListener("click", () => {
                    modal.style.display = "block";
                    overlay.style.display = "block";

                    if (emailInput) emailInput.value = "";
                    if (emailError) emailError.style.display = "none";
                    if (emailSuccess) emailSuccess.style.display = "none";
                    if (copySuccess) copySuccess.style.display = "none";
                });
            }, 0);

            await new Promise(resolve => setTimeout(resolve, 10));

            const btnImg = document.querySelector('.rnb-toggle-btn img');
            const body = document.body;
            if (btnImg) {
                btnImg.style.transform = 'rotate(0deg)';
                document.querySelector('.rnb-toggle-btn').addEventListener('click', () => {
                    const isClosed = body.classList.contains('rnb-closed');
                    body.classList.toggle('rnb-closed');
                    btnImg.style.transform = isClosed ? 'rotate(0deg)' : 'rotate(180deg)';
                    // 캘린더 잘림 현상 수정 코드
                    setTimeout(() => {
                    if (window.calendar) { // window.calendar로 접근하여 전역 변수임을 명시
                        window.calendar.relayout();
                    }
                }, 350);
                });
            }

            const getImagePath = (img) => {
                if (!img) return "/images/default.png";
                if (img.startsWith("/") || img.startsWith("http")) return img;
                return `/images/${img}`;
            };

            const profileRes = await fetch(`/api/workspaces/${workspaceCd}/profile`);
            if (!profileRes.ok) throw new Error("프로필 API 실패");
            const myProfile = await profileRes.json();

            document.getElementById("myProfileImg").src = getImagePath(myProfile.userImg);
            document.getElementById("myProfileName").textContent = myProfile.userNickname || "이름없음";
            document.getElementById("myProfileRole").textContent = myProfile.position || "직급없음";
            document.getElementById("myProgressBar").style.width = (myProfile.progress || 0) + "%";
            document.getElementById("myProgressPercent").textContent = (myProfile.progress || 0) + "%";

            const mpImg = document.querySelector(".mini-profile .mpImg");
            const mpName = document.querySelector(".mini-profile .mp-name");
            const mpRole = document.querySelector(".mini-profile .mp-role");

            if (mpImg) mpImg.src = getImagePath(myProfile.userImg);
            if (mpName) mpName.textContent = myProfile.userNickname || "이름없음";
            if (mpRole) mpRole.textContent = myProfile.position || "직급없음";





            document.addEventListener("DOMContentLoaded", function () {
                const toggleBtn = document.getElementById("statusToggleBtn");
                const dropdown = document.getElementById("statusDropdown");
                const icon = document.getElementById("statusIcon");
                const text = document.getElementById("statusText");

                // ✅ 드롭다운 열고 닫기
                toggleBtn.addEventListener("click", (e) => {
                    e.stopPropagation();
                    dropdown.style.display = dropdown.style.display === "block" ? "none" : "block";
                });

                document.addEventListener("click", () => {
                    dropdown.style.display = "none";
                });

                // ✅ 상태 옵션 클릭 → 실제 서버에 PATCH 요청
                const options = dropdown.querySelectorAll(".status-option");
                options.forEach(option => {
                    option.addEventListener("click", () => {
                        const newText = option.getAttribute("data-text");
                        let newStatus = "online";

                        if (newText === "자리 비움") newStatus = "away";
                        else if (newText === "오프라인") newStatus = "offline";

                        // 실제 DB 상태 변경 + UI 반영
                        changeStatus(newStatus);

                        dropdown.style.display = "none";
                    });
                });
            });

            // ✅ UI 반영 함수 (상태 텍스트 & 아이콘 & 표시 텍스트)
            function updateStatusDisplay(status) {
                const display = document.querySelector(".user-status-display");
                const icon = document.getElementById("statusIcon");
                const text = document.getElementById("statusText");

                const statusMap = {
                    online: {
                        label: "온라인",
                        icon: "/images/green_circle.png"
                    },
                    away: {
                        label: "자리 비움",
                        icon: "/images/red_circle.png"
                    },
                    offline: {
                        label: "오프라인",
                        icon: "/images/gray_circle.png"
                    }
                };

                const { label, icon: iconSrc } = statusMap[status.toLowerCase()] || statusMap["online"];

                if (display) display.textContent = label;
                if (icon) icon.src = iconSrc;
                if (text) text.textContent = label;

                console.log("✅ 상태 표시됨:", label);
            }








            const memberRes = await fetch(`/api/workspaces/${workspaceCd}/members`);
            if (!memberRes.ok) throw new Error("멤버 API 실패");
            const data = await memberRes.json();
            const members = data.members || [];

            document.getElementById("memberCount").textContent = members.length;
            const memberContainer = document.getElementById("memberListContainer");

            members.forEach(member => {
                const memberDiv = document.createElement("div");
                memberDiv.classList.add("member");
                memberDiv.innerHTML = `
                    <a href="javascript:void(0);" class="member-link" onclick="showProfileModel('${member.userId}')">
                        <img src="${getImagePath(member.userImg)}" alt="멤버이미지">
                        <div class="info">
                            <span class="m-name">${member.userNickname}</span>
                            <span class="m-role">${member.position}</span>
                        </div>
                    </a>
                `;
                memberContainer.appendChild(memberDiv);
            });

            const infoRes = await fetch(`/api/workspaces/${workspaceCd}/info`);
            if (!infoRes.ok) throw new Error("워크스페이스 정보 API 실패");
            const workspaceInfo = await infoRes.json();

            const inviteBtn = document.querySelector(".invite-member");
            const modal = document.getElementById("inviteModal");
            const overlay = document.getElementById("inviteOverlay");
            const closeBtn = modal?.querySelector(".close-btn");

            const emailInput = document.getElementById("inviteEmail");
            const emailError = document.getElementById("emailError");
            const emailSuccess = document.getElementById("emailSuccess");
            const copySuccess = document.getElementById("copySuccess");
            const sendBtn = document.querySelector(".send-btn");
            const copyBtn = document.querySelector(".copy-btn");
            const inviteCode = document.getElementById("inviteCode");
            const workspaceNameHeader = document.getElementById("workspaceNameHeader");

            workspaceNameHeader.textContent = `${workspaceInfo.workspaceName}(으)로 초대하기`;
            inviteCode.textContent = workspaceInfo.inviteCode;

            const closeModal = () => {
                modal.style.display = "none";
                overlay.style.display = "none";
            };
            closeBtn?.addEventListener("click", closeModal);
            overlay?.addEventListener("click", (e) => {
                if (e.target === overlay) closeModal();
            });
            modal?.addEventListener("click", (e) => e.stopPropagation());

            sendBtn?.addEventListener("click", () => {
                const email = emailInput.value.trim();
                const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

                emailError.style.display = "none";
                emailSuccess.style.display = "none";

                if (!regex.test(email)) {
                    emailError.style.display = "block";
                    return;
                }

                fetch("/api/workspaces/invite-email", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email: email, inviteCode: workspaceInfo.inviteCode })
                })
                    .then(res => {
                        if (!res.ok) throw new Error("전송 실패");
                        return res.text();
                    })
                    .then(() => {
                        emailSuccess.style.display = "block";
                    })
                    .catch(err => {
                        emailError.textContent = "전송 실패: " + err.message;
                        emailError.style.display = "block";
                    });
            });



            copyBtn?.addEventListener("click", () => {
                navigator.clipboard.writeText(workspaceInfo.inviteCode)
                    .then(() => copySuccess.style.display = "block")
                    .catch(() => alert("복사 실패"));
            });

              document.getElementById("toggleEditBtn").addEventListener("click", () => {
                  const isEditMode = document.getElementById("toggleEditBtn").dataset.editing === "true";
                  const imgEl = document.getElementById("viewProfileImg");

                  if (!isEditMode) {
                      // 편집모드 전환
                      const fields = ["Nickname", "Phone", "Position", "Email"];
                      fields.forEach(f => {
                          const el = document.getElementById(`view${f}`);
                          const text = el.textContent;
                          const input = document.createElement("input");
                          input.type = "text";
                          input.id = `edit${f}`;
                          input.classList.add("edit-input");
                          input.value = text === "-" ? "" : text;
                          el.replaceWith(input);
                      });

                      imgEl.style.cursor = "pointer";
                      const fileInput = document.createElement("input");
                      fileInput.type = "file";
                      fileInput.accept = "image/*";
                      fileInput.style.display = "none";
                      fileInput.id = "editProfileImgInput";
                      imgEl.parentNode.appendChild(fileInput);

                      imgEl.addEventListener("click", () => fileInput.click());
                      fileInput.addEventListener("change", () => {
                          const file = fileInput.files[0];
                          if (file) {
                              const reader = new FileReader();
                              reader.onload = (e) => {
                                  imgEl.src = e.target.result;
                              };
                              reader.readAsDataURL(file);
                          }
                      });

                      document.getElementById("toggleEditBtn").textContent = "저장하기";
                      document.getElementById("toggleEditBtn").dataset.editing = "true";

                  } else {
                      // 저장하기
                      const formData = new FormData();
                      formData.append("userNickname", document.getElementById("editNickname").value);
                      formData.append("phoneNum", document.getElementById("editPhone").value);
                      formData.append("position", document.getElementById("editPosition").value);
                      formData.append("email", document.getElementById("editEmail").value);

                      // 🔹 deptCd는 수정 불가지만 서버에서 필수이므로 현재 값 전달
                      const deptSpan = document.getElementById("viewDept");
                      if (deptSpan && deptSpan.dataset.deptCd) {
                          formData.append("deptCd", deptSpan.dataset.deptCd);
                      } else {
                          alert("부서 정보가 없습니다.");
                          return;
                      }

                      const fileInput = document.getElementById("editProfileImgInput");
                      if (fileInput && fileInput.files.length > 0) {
                          formData.append("userImg", fileInput.files[0]);
                      }

                      fetch(`/workspace/${workspaceCd}/set-profile2`, {
                          method: "POST",
                          body: formData
                      })
                      .then(res => res.text())
                      .then(msg => {


                          if (msg === "success") {
                              alert("수정 완료!");
                              document.getElementById("myInfoBtn").click(); // reload trigger
                          } else {
                              throw new Error(msg);
                          }
                      })
                      .catch(err => {
                          console.error("❌ 저장 실패:", err);
                          alert("저장 실패: " + err.message);
                      });
                  }
              });




        } catch (err) {
            console.error("🔴 RNB 전체 로딩 중 에러:", err);
        }
    });

    window.showProfileModel = showProfileModel;
    window.closeProfileModal = closeProfileModal;
})();

function bindStatusChangeEvents() {
    const toggleBtn = document.getElementById("statusToggleBtn");
    const dropdown = document.getElementById("statusDropdown");
    const icon = document.getElementById("statusIcon");
    const text = document.getElementById("statusText");

    if (!toggleBtn || !dropdown || !icon || !text) {
        console.warn("🔴 상태 관련 요소가 없습니다 (rnb 미삽입 시)");
        return;
    }

    // 드롭다운 열기
    toggleBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        dropdown.style.display = dropdown.style.display === "block" ? "none" : "block";
    });

    document.addEventListener("click", () => {
        dropdown.style.display = "none";
    });

    // 상태 클릭 시 실제 요청
    const options = dropdown.querySelectorAll(".status-option");
    options.forEach(option => {
        option.addEventListener("click", () => {
            const newText = option.getAttribute("data-text");
            let newStatus = "online";
            if (newText === "자리 비움") newStatus = "away";
            else if (newText === "오프라인") newStatus = "offline";

            changeStatus(newStatus);
            dropdown.style.display = "none";
        });
    });
}
