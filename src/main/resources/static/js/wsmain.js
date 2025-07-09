function formatSecondsToHHMMSS(seconds) {
    const h = String(Math.floor(seconds / 3600)).padStart(2, '0');
    const m = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0');
    const s = String(seconds % 60).padStart(2, '0');
    return `${h}시간 ${m}분 ${s}초`;
}

document.addEventListener("DOMContentLoaded", function () {
    const userId = localStorage.getItem("userId");
    const workspaceCd = localStorage.getItem("workspaceCd");

    console.log("📦 로딩 시작 - userId:", userId, ", workspaceCd:", workspaceCd);

    if (!userId || !workspaceCd) {
        console.warn("⚠️ userId 또는 workspaceCd가 localStorage에 없습니다.");
        return;
    }

    document.querySelectorAll(".close-button").forEach(btn => {
        btn.addEventListener("click", function () {
            this.closest(".modal").style.display = "none";
        });
    });

    // ✅ 상단 배너 정보 세팅
    fetch(`/api/workspaces/${workspaceCd}/info`)
        .then(res => {
            if (!res.ok) {
                throw new Error(`HTTP error! status: ${res.status}`);
            }
            return res.json();
        })
        .then(data => {
            console.log("✅ 워크스페이스 정보:", data);

            // 워크스페이스 이름
            const titleElement = document.querySelector('.workspace-title');
            if (titleElement) {
                titleElement.textContent = data.workspaceName || '워크스페이스 이름';
            } else {
                console.error("❌ workspace-title 엘리먼트를 찾을 수 없습니다");
            }

            // 마감 날짜
            document.getElementById("project_endDate").textContent = data.dueDateFormatted;

            // ✅ D-day 숫자 조합해서 표시 (ex: D-8)
            const ddayElem = document.getElementById("top-banner-dday");
            if (typeof data.dday === 'number') {
                ddayElem.textContent = `D-${data.dday}`;
            } else if (typeof data.dday === 'string' && data.dday.startsWith('D-')) {
                ddayElem.textContent = data.dday;
            } else {
                ddayElem.textContent = `D-${data.dday || "?"}`;
            }

            // 오늘 날짜
            const dateElem = document.getElementById("top-banner-date");
            const today = new Date();
            const formatter = new Intl.DateTimeFormat('ko-KR', {
                month: 'long',
                day: 'numeric',
                weekday: 'long'
            });
            dateElem.textContent = formatter.format(today);

            // ✅ 진행도 퍼센트 설정
            const progressFill = document.getElementById("progress-fill");

            let progressPercent = parseInt(data.progressPercent);
            if (isNaN(progressPercent) || progressPercent < 0) progressPercent = 0;
            if (progressPercent > 100) progressPercent = 100;

            progressFill.style.width = `${progressPercent}%`;
        })
        .catch(err => {
            console.error("❌ 상단 배너 정보 로딩 실패:", err);
        });


        // ✅ 최근 활동 알림 불러오기
        fetch(`/api/workspaces/${workspaceCd}/notifications`)
            .then(res => res.json())
            .then(data => {
                const container = document.querySelector(".activity");
                container.innerHTML = "<h2>최근활동</h2>";

                if (!data || data.length === 0) {
                    container.innerHTML += "<div class='log'>최근 활동이 없습니다.</div>";
                    return;
                }

                data.forEach(noti => {
                    const div = document.createElement("div");
                    div.classList.add("log");

                    const initial = noti.senderName?.charAt(0) || "?";
                    const content = noti.content || "알 수 없는 활동";

                    div.innerHTML = `<span class="badge">${initial}</span> ${noti.senderName}님이 ${content}`;
                    container.appendChild(div);
                });
            })
            .catch(err => {
                console.error("❌ 최근 활동 알림 불러오기 실패:", err);
                const container = document.querySelector(".activity");
                container.innerHTML += "<div class='log'>활동 정보를 불러올 수 없습니다.</div>";
            });


    // ✅ 누적 접속 시간 로딩
    fetch(`/api/events/${workspaceCd}/usage-time`)
        .then(res => res.json())
        .then(seconds => {
            const formatted = formatSecondsToHHMMSS(seconds);
            document.getElementById("usage-time").textContent = formatted;
        })
        .catch(err => {
            console.error("사용 시간 로드 실패:", err);
            document.getElementById("usage-time").textContent = "불러오기 실패";
        });

    // ✅ 오늘 일정
    fetch(`/api/events/today?userId=${userId}&workspaceCd=${workspaceCd}`)
        .then(response => response.json())
        .then(data => {
            const list = document.getElementById("user-events-list");
            list.innerHTML = "";

            if (!data || data.length === 0) {
                list.innerHTML = "<li>오늘 등록된 일정이 없습니다.</li>";
                return;
            }

            data.forEach(event => {
                const start = new Date(event.startDatetime);
                const time = start.toLocaleTimeString('ko-KR', {
                    hour: '2-digit',
                    minute: '2-digit'
                });

                const li = document.createElement("li");
                li.innerHTML = `<strong>${time}</strong> - ${event.title}`;
                list.appendChild(li);
            });
        })
        .catch(error => {
            console.error("❗ 오늘 일정 오류:", error);
            document.getElementById("user-events-list").innerHTML = "<li>일정 정보를 불러올 수 없습니다.</li>";
        });

    // ✅ 이번 주 완료 일정
    fetch(`/api/events/this-week-completed-count?workspaceCd=${workspaceCd}`)
        .then(res => res.json())
        .then(count => {
            document.getElementById('completed-this-week').innerText = `${count}`;
        })
        .catch(err => {
            document.getElementById('completed-this-week').innerText = '불러오기 실패';
        });

    // ✅ 이번 주 다가오는 일정
    fetch(`/api/events/this-week-upcoming-count?workspaceCd=${workspaceCd}`)
        .then(res => res.json())
        .then(count => {
            document.getElementById('upcoming-this-week').innerText = `${count}`;
        })
        .catch(err => {
            document.getElementById('upcoming-this-week').innerText = '불러오기 실패';
        });

    // ✅ 이번 주 생성된 일정
    fetch(`/api/events/this-week-created-count?workspaceCd=${workspaceCd}`)
        .then(res => res.json())
        .then(count => {
            document.getElementById('created-this-week').innerText = `${count}`;
        })
        .catch(err => {
            document.getElementById('created-this-week').innerText = '불러오기 실패';
        });

    // ✅ 사용자 정보 로딩
//    fetch(`/api/workspaces/${workspaceCd}/member/${userId}`)
//        .then(res => res.json())
//        .then(user => {
//            document.getElementById("mini-profile-img").src = user.userImg || "/images/default.png";
//            document.getElementById("mini-profile-name").textContent = user.userNickname || user.userId;
//            document.getElementById("mini-profile-role").textContent = user.userRole || "MEMBER";
//        });

    // ✅ 상태 불러오기
    fetch(`/api/workspaces/${workspaceCd}/member/${userId}/status`)
        .then(res => {
            if (!res.ok) throw new Error("서버 응답 오류");
            return res.text();
        })
        .then(status => {
            console.log( "status  ===>  ", status);
            //updateStatusDisplay(status);
              setTimeout(() => {

              updateStatusDisplay(status);

              } ,0);
        })
        .catch(err => {
            console.error("❌ 상태 불러오기 실패:", err);
           // updateStatusDisplay("online");
        });
});


function showUserDetailModal(userId) {
    const workspaceCd = localStorage.getItem("workspaceCd");

    fetch(`/api/workspaces/${workspaceCd}/member/${userId}`)
        .then(res => res.json())
        .then(user => {
            document.getElementById("detail-img").src = user.userImg || "/images/default.png";
            document.getElementById("detail-name").textContent = user.userNickname || user.userId;
            document.getElementById("detail-email").textContent = user.email || "-";
            document.getElementById("detail-phone").textContent = user.phoneNum || "-";
            document.getElementById("detail-dept").textContent = user.deptNm || "-";
            document.getElementById("detail-position").textContent = user.position || "-";
            document.getElementById("detail-status").textContent = user.statusMsg || "-";

            document.getElementById("user-detail-modal").style.display = "block";
        })
        .catch(err => {
            alert("사용자 정보를 불러오지 못했습니다.");
            console.error(err);
        });
}

function goToMyPage() {
    const workspaceCd = localStorage.getItem("workspaceCd");
    const userId = localStorage.getItem("userId");

    fetch(`/api/workspaces/${workspaceCd}/member/${userId}`)
        .then(res => res.json())
        .then(user => {
            document.getElementById("my-img").src = user.userImg || "/images/default.png";
            document.getElementById("my-name").textContent = user.userNickname || user.userId;
            document.getElementById("my-email").textContent = user.email || "-";
            document.getElementById("my-phone").textContent = user.phoneNum || "-";
            document.getElementById("my-dept").textContent = user.deptNm || "-";
            document.getElementById("my-position").textContent = user.position || "-";
            document.getElementById("my-status").textContent = user.statusMsg || "-";

            document.getElementById("edit-email").value = user.email || "";
            document.getElementById("edit-nickname").value = user.userNickname || "";
            document.getElementById("edit-phone").value = user.phoneNum || "";
            document.getElementById("edit-dept").value = user.deptNm || "";
            document.getElementById("edit-position").value = user.position || "";
            document.getElementById("edit-status").value = user.statusMsg || "";

            document.getElementById("my-info-modal").style.display = "flex";
        })
        .catch(err => {
            alert("사용자 정보를 불러오지 못했습니다.");
            console.error(err);
        });
}

function openEditModal() {
    document.getElementById("my-info-modal").style.display = "none";
    document.getElementById("edit-info-modal").style.display = "flex";
    loadDepartmentOptions();
}

function submitEdit() {
    const workspaceCd = localStorage.getItem("workspaceCd");

    const formData = new FormData();
    formData.append("userNickname", document.getElementById("edit-nickname").value);
    formData.append("email", document.getElementById("edit-email").value);
    formData.append("phoneNum", document.getElementById("edit-phone").value);
    formData.append("deptCd", document.getElementById("edit-dept").value);
    formData.append("position", document.getElementById("edit-position").value);
    formData.append("statusMsg", document.getElementById("edit-status").value);

    const fileInput = document.getElementById("edit-img");
    if (fileInput.files.length > 0) {
        formData.append("userImg", fileInput.files[0]);
    }

    fetch(`/workspace/${workspaceCd}/set-profile`, {
        method: "POST",
        body: formData
    })
        .then(res => res.text())
        .then(msg => {
            if (msg === "success") {
                alert("수정 완료!");
                document.getElementById("edit-info-modal").style.display = "none";
                goToMyPage();
            } else {
                throw new Error(msg);
            }
        })
        .catch(err => {
            console.error("❌ 수정 실패:", err);
            alert("수정 실패: " + err.message);
        });
}

function closeModal(id) {
    document.getElementById(id).style.display = "none";
}

function logout() {
    window.location.href = "/workspace";
}

function showStatus() {
    toggleStatusMenu();
}

function toggleStatusMenu() {
    const modal = document.getElementById("status-modal");
    modal.style.display = modal.style.display === "block" ? "none" : "block";
}

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


   // ✅ 실제 상태 PATCH 요청
            function changeStatus(newStatus) {
                const workspaceCd = localStorage.getItem("workspaceCd");
                const userId = localStorage.getItem("userId");

                fetch(`/api/workspaces/${workspaceCd}/member/${userId}/status`, {
                    method: "PATCH",
                    headers: { "Content-Type": "text/plain" },
                    body: newStatus
                })
                    .then(res => {
                        if (!res.ok) throw new Error("업데이트 실패");
                        return res.text(); // ✅ 절대 res.json() 쓰지 마!
                    })
                    .then(msg => {
                        console.log("✅ 상태 변경 성공:", msg);
                        updateStatusDisplay(newStatus);  // UI 반영
                    })

            }

function loadDepartmentOptions() {
    const workspaceCd = localStorage.getItem("workspaceCd");
    const select = document.getElementById("edit-dept");

    fetch(`/api/workspaces/${workspaceCd}/departments`)
        .then(res => res.json())
        .then(depts => {
            select.innerHTML = "";
            depts.forEach(dept => {
                const option = document.createElement("option");
                option.value = dept.deptCd;
                option.textContent = dept.deptNm;
                select.appendChild(option);
            });
        })
        .catch(err => {
            console.error("부서 목록 불러오기 실패", err);
        });
}

document.addEventListener("DOMContentLoaded", function () {
    const statusOptions = document.querySelectorAll(".status-option");

    statusOptions.forEach(option => {
        option.addEventListener("click", () => {
            const text = option.getAttribute("data-text");
            let newStatus = "online";
            if (text === "자리 비움") newStatus = "away";
            else if (text === "오프라인") newStatus = "offline";

            changeStatus(newStatus);
        });
    });
});
