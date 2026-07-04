# Hướng Dẫn Triển Khai (Deployment Guide) - WMS
Hệ thống Quản lý Kho (Warehouse Management System) trên VPS Azure & Cloudflare Tunnel.

---

## 🏗️ Kiến Trúc Hệ Thống (Production)

Trong môi trường Production, để bảo mật tối đa, chúng ta sẽ thiết lập hệ thống theo mô hình:
1. **Cloudflare Tunnel (`cloudflared`)** đóng vai trò là cổng bảo mật, kết nối trực tiếp từ VPS ra mạng Cloudflare (Outbound).
2. **Không cần mở port 80/443 public** trên Firewall (NSG) của Azure.
3. **Nginx** bên trong container `frontend` sẽ nhận traffic từ Tunnel ở port `3000`, phân phối các yêu cầu giao diện (`/`) và chuyển tiếp (proxy) các API request `/api/*` trực tiếp tới container `backend:8080` qua mạng nội bộ của Docker.

```
[ Người Dùng ] ──(HTTPS)──> [ Cloudflare Edge ] 
                                  │
                          (Cloudflare Tunnel)
                                  ▼
[ VPS Azure ] ──────────> [ cloudflared daemon ]
                                  │ (HTTP)
                                  ▼
                     [ frontend (Nginx:3000) ]
                       /                 \
             (Static Files)         (Proxy API /api/*)
             /                             \
     [ React SPA App ]             [ backend (Spring Boot:8080) ]
                                            │
                                            ▼
                                  [ db (PostgreSQL:5432) ]
```

---

## 📋 Các Bước Thực Hiện Chi Tiết

### Bước 1: Hoàn Tất Trỏ Domain Về Cloudflare
Như trạng thái hiện tại trên tài khoản Cloudflare của anh:
1. Chờ Cloudflare cập nhật Name Servers từ nhà cung cấp tên miền của anh (quá trình này thường mất từ 5 - 30 phút, đôi khi lâu hơn tùy nhà mạng).
2. Khi trạng thái chuyển sang **Active (màu xanh)**, tên miền `manager-warehouse.online` của anh đã sẵn sàng hoạt động qua Cloudflare.

---

### Bước 2: Thiết Lập VPS (Azure Ubuntu Server 24.04)

1. **Đăng nhập vào VPS bằng SSH:**
   Sử dụng Private Key SSH mà anh đã tạo khi khởi tạo VM trên Azure:
   ```bash
   ssh -i /path/to/your/private_key.pem azureuser@<IP_CUA_VPS>
   ```

2. **Cập nhật hệ thống và cài đặt Docker:**
   Chạy các lệnh sau trên terminal VPS của anh:
   ```bash
   # Cập nhật danh sách package
   sudo apt update && sudo apt upgrade -y

   # Cài đặt các thư viện cần thiết
   sudo apt install -y apt-transport-https ca-certificates curl software-properties-common gnupg lsb-release

   # Thêm GPG key chính thức của Docker
   sudo mkdir -p /etc/apt/keyrings
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

   # Thiết lập Docker repository
   echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

   # Cài đặt Docker Engine & Docker Compose
   sudo apt update
   sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

   # Cho phép user azureuser chạy Docker không cần sudo (Cần reconnect SSH sau khi chạy)
   sudo usermod -aG docker $USER
   ```

---

### Bước 3: Đưa Code Lên VPS & Cấu Hình Môi Trường

1. **Tải mã nguồn lên VPS:**
   Anh có thể clone repository trực tiếp từ GitHub trên VPS:
   ```bash
   git clone <URL_REPO_CUA_ANH> /app/manager-warehouse
   cd /app/manager-warehouse
   ```

   Workflow CD mặc định sử dụng đúng thư mục `/app/manager-warehouse`. Nếu clone
   repository ở vị trí khác, tạo GitHub Actions variable `VPS_APP_DIR` với đường
   dẫn tuyệt đối tới repository trên VPS.

2. **Tạo file cấu hình môi trường `.env`:**
   Dựa trên file mẫu `docker.env.example`, tạo file `.env` tại thư mục gốc `/app/manager-warehouse`:
   ```bash
   cp docker.env.example .env
   nano .env
   ```

   **Nội dung cấu hình mẫu (Nếu chọn PostgreSQL chạy cùng Docker trên VPS):**
   ```env
   # Database credentials (cho Postgres Container)
   DB_USER=wms_user
   DB_PASSWORD=Đặt_Một_Mật_Khẩu_Cực_Kỳ_Bảo_Mật

   # JWT secret dùng để mã hóa token đăng nhập (Tối thiểu 64 ký tự ngẫu nhiên)
   JWT_SECRET=Thay_bằng_chuỗi_hash_ngẫu_nhiên_rất_dài_để_bảo_mật_hệ_thống

   # Cấu hình Mail gửi thông báo (SMTP)
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=mật_khẩu_ứng_dụng_gmail

   # Cổng chạy frontend
   FRONTEND_PORT=3000
   ```

---

### Bước 4: Chuẩn Bị Thư Mục Release Và Backup

Production không build source trên VPS. Workflow GitHub Actions sẽ publish image
đã kiểm tra lên GHCR rồi truyền đúng image digest cho VPS. Chuẩn bị các thư mục
bền vững trước lần deploy đầu tiên:

```bash
sudo mkdir -p /app/backups/manager-warehouse /app/manager-warehouse/.release
sudo chown -R "$USER":"$USER" /app/backups/manager-warehouse /app/manager-warehouse/.release
chmod 700 /app/backups/manager-warehouse /app/manager-warehouse/.release
```

Đăng nhập GHCR một lần để xác nhận PAT có quyền đọc package. Không ghi PAT vào
shell history; workflow sẽ đăng nhập lại bằng secret được mask:

```bash
printf '%s' '<GHCR_READ_PACKAGES_PAT>' | docker login ghcr.io -u '<GITHUB_USERNAME>' --password-stdin
docker logout ghcr.io
```

`compose.prod.yaml` bắt buộc `BACKEND_IMAGE` và `FRONTEND_IMAGE` dạng
`ghcr.io/...@sha256:...`; không dùng mutable tag như `latest`.

---

### Bước 5: Cấu Hình GitHub Actions CD

Trong repository GitHub, vào **Settings → Secrets and variables → Actions** và
tạo các repository/environment secrets sau:

* `VPS_HOST`: IP hoặc hostname của VPS.
* `VPS_USERNAME`: user SSH có quyền chạy Docker.
* `VPS_SSH_KEY`: nội dung private key SSH.
* `VPS_SSH_FINGERPRINT`: fingerprint SHA256 của SSH host key.
* `GHCR_USERNAME`: GitHub user hoặc machine user có quyền đọc package.
* `GHCR_TOKEN`: fine-grained PAT chỉ có quyền tối thiểu để đọc GHCR package.

Tạo GitHub Environment tên `production`, cấu hình **Required reviewers** và bật
**Prevent self-review**. Đặt
các secrets trên ở environment scope khi có thể. Người phê duyệt production phải
khác người tạo thay đổi để giữ maker/checker separation.

Tạo các environment variables:

* `VPS_APP_DIR`: mặc định `/app/manager-warehouse`.
* `VPS_BACKUP_DIR`: mặc định `/app/backups/manager-warehouse`.
* `PRODUCTION_URL`: ví dụ `https://manager-warehouse.online`, dùng cho public smoke test.

Lấy fingerprint trực tiếp từ VPS bằng lệnh:

```bash
sudo ssh-keygen -l -f /etc/ssh/ssh_host_ed25519_key.pub | awk '{print $2}'
```

Workflow `.github/workflows/deploy.yml` chạy khi code được push/merge vào `main`
hoặc khi bấm **Run workflow**. Quy trình bắt buộc:

1. Backend tests và frontend lint/test/build.
2. Build backend/frontend images một lần và publish lên GHCR theo commit SHA.
3. Scan image; lỗ hổng HIGH/CRITICAL chưa có bản sửa sẽ chặn deploy.
4. Chờ required reviewer phê duyệt GitHub Environment `production`.
5. VPS tạo backup PostgreSQL có checksum trước khi thay đổi containers.
6. Pull đúng image digest, deploy, kiểm tra container health và domain public.
7. Nếu kiểm tra thất bại, rollback application images về release trước; workflow
   không tự động downgrade Flyway hoặc restore database.

VPS phải có file `.env`; workflow không truyền database/JWT/mail secrets từ
GitHub xuống VPS. `GHCR_TOKEN` chỉ được dùng để pull image và không được ghi log.

### Ngoại Lệ Bảo Mật Có Thời Hạn

Các CVE tồn tại do constitution khóa Spring Boot 3.4.5 được ghi tại
`.trivyignore.yaml` theo quyết định chấp nhận rủi ro ngày 2026-07-03. Ngoại lệ:

* hết hạn ngày **2026-08-02**;
* chỉ áp dụng cho CVE và target đã liệt kê;
* vẫn hiển thị trong output scan dưới dạng suppressed;
* không bỏ qua HIGH/CRITICAL mới;
* phải được xóa khi stack được nâng cấp hoặc trước ngày hết hạn.

Không gia hạn bằng cách đổi ngày đơn thuần. Mọi gia hạn cần Release Approver đánh
giá lại CVE, ghi lý do và tạo PR review riêng.

---

### Bước 6: Cấu Hình Cloudflare Tunnel

Thay vì mở cổng 3000 trên Azure firewall, chúng ta sẽ cài đặt tác vụ Tunnel để định tuyến traffic từ tên miền an toàn về cổng local `3000` của VPS.

1. **Tạo Tunnel trên Cloudflare Dashboard:**
   * Truy cập **Cloudflare Zero Trust** (từ menu bên trái màn hình Cloudflare Dashboard chính của anh).
   * Chọn **Networks** -> **Tunnels** -> Click **Create a tunnel**.
   * Chọn **Cloudflare Tunnel (connector)** -> Click **Next**.
   * Đặt tên cho tunnel (Ví dụ: `wms-vps-tunnel`) -> Click **Save tunnel**.

2. **Cài đặt và chạy Connector trên VPS:**
   * Chọn tab **Debian (64-bit)** (vì VPS chạy Ubuntu).
   * Sao chép đoạn lệnh cài đặt được sinh ra bởi Cloudflare (bao gồm Token bí mật của anh). Nó sẽ có dạng như sau:
     ```bash
     curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb && sudo dpkg -i cloudflared.deb
     sudo cloudflared service install <YOUR_TUNNEL_TOKEN>
     ```
   * Chạy câu lệnh trên trên VPS để khởi động Tunnel. Trạng thái Tunnel trên Dashboard Cloudflare sẽ hiển thị **Active (màu xanh)**.

3. **Cấu hình định tuyến tên miền (Public Hostname):**
   * Trong giao diện cấu hình Tunnel trên Cloudflare, click sang tab **Public Hostname**.
   * Click **Add a public hostname**.
   * Cấu hình như sau:
     * **Subdomain:** Để trống (hoặc điền `www` tùy ý anh).
     * **Domain:** Chọn `manager-warehouse.online`.
     * **Path:** Để trống.
     * **Service Type:** Chọn `HTTP`.
     * **URL:** Điền `localhost:3000` (đây là cổng chạy dịch vụ frontend chứa proxy của Nginx trên VPS).
   * Click **Save hostname**.

*Lưu ý: Cloudflare sẽ tự động tạo một bản ghi CNAME trỏ tên miền của anh về Tunnel, anh không cần cấu hình thêm bản ghi DNS thủ công.*

---

## 🧪 Xác Minh & Kiểm Tra Sau Triển Khai (Post-Deploy)

1. Truy cập thử tên miền: `https://manager-warehouse.online`
2. Kiểm tra chứng chỉ bảo mật SSL (Xem có biểu tượng ổ khóa xanh trên trình duyệt).
3. Đăng nhập thử vào hệ thống WMS để kiểm tra xem Nginx proxy các API request tới Backend và Database có thông suốt hay không.
4. Đối chiếu image đang chạy với release digest:
   ```bash
   docker inspect --format '{{.Config.Image}}' \
     "$(docker compose --env-file .env --env-file .release/current.env -f compose.prod.yaml ps -q backend)"
   docker inspect --format '{{.Config.Image}}' \
     "$(docker compose --env-file .env --env-file .release/current.env -f compose.prod.yaml ps -q frontend)"
   ```
5. Kiểm tra backup mới nhất có file `.dump` khác rỗng và file checksum `.sha256`.

### Rollback Và Sự Cố

Script deploy tự rollback application khi health/smoke test thất bại và
`.release/previous.env` tồn tại. Để chạy lại rollback ứng dụng có kiểm soát:

```bash
cd /app/manager-warehouse
APP_DIR="$PWD" \
BACKUP_DIR=/app/backups/manager-warehouse \
PREVIOUS_RELEASE_ENV="$PWD/.release/previous.env" \
PUBLIC_URL=https://manager-warehouse.online \
scripts/deploy/rollback-release.sh
```

Rollback không đổi migration history. Nếu previous application không tương thích
với schema hiện tại hoặc rollback vẫn unhealthy, dừng thao tác và mở incident.
Database restore chỉ được thực hiện sau khi đánh giá dữ liệu phát sinh kể từ
backup và có phê duyệt riêng; không chạy restore tự động từ workflow.
