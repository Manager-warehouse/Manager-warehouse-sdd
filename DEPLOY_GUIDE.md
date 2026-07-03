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

### Bước 4: Khởi Chạy Ứng Dụng Bằng Docker Compose

Sử dụng file cấu hình sản xuất `compose.prod.yaml` đã tích hợp sẵn PostgreSQL để build và chạy ứng dụng ở chế độ nền (daemon):

```bash
# Build và khởi chạy tất cả services (frontend, backend, postgres database)
docker compose -f compose.prod.yaml up -d --build
```

**Cách kiểm tra trạng thái hoạt động:**
* Xem các container đang chạy: `docker compose -f compose.prod.yaml ps`
* Xem log của backend: `docker compose -f compose.prod.yaml logs -f backend`
* Xem log của frontend (Nginx): `docker compose -f compose.prod.yaml logs -f frontend`

---

### Bước 5: Cấu Hình GitHub Actions CD

Trong repository GitHub, vào **Settings → Secrets and variables → Actions** và
tạo các repository/environment secrets sau:

* `VPS_HOST`: IP hoặc hostname của VPS.
* `VPS_USERNAME`: user SSH có quyền chạy Docker.
* `VPS_SSH_KEY`: nội dung private key SSH.
* `VPS_SSH_FINGERPRINT`: fingerprint SHA256 của SSH host key.

Lấy fingerprint trực tiếp từ VPS bằng lệnh:

```bash
sudo ssh-keygen -l -f /etc/ssh/ssh_host_ed25519_key.pub | awk '{print $2}'
```

Workflow `.github/workflows/deploy.yml` chạy khi code được push/merge vào `main`
hoặc khi bấm **Run workflow**. Nó chỉ deploy sau khi backend test, frontend
test/build và Docker image build đều thành công. VPS phải có file `.env`; workflow
không truyền mật khẩu ứng dụng từ GitHub xuống VPS.

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
4. Chạy lệnh `/save-brain` trên AI chat này sau khi hoàn tất để ghi nhớ trạng thái deploy.
