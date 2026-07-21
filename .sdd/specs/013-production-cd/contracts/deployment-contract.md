# Deployment Contract

## Inputs

- Release manifest hợp lệ theo `release-manifest.schema.json`.
- Production environment approval.
- Registry read access, SSH connection và pinned host fingerprint.
- VPS application directory, production Compose file và server-side `.env` tồn tại.
- Backup directory tồn tại, ghi được và có đủ dung lượng.

## Preconditions

1. Backend tests, frontend lint/test/build, Compose validation và image scan đạt.
2. Backend/frontend image digests khớp release manifest.
3. Không có deploy khác đang chạy.
4. Production checkout/config không có drift bị cấm.
5. Backup được tạo, checksum xác minh và metadata lưu thành công.
6. Production drift chỉ gồm `.env`, release/backup artifacts và Docker runtime data đã khai báo; tracked deployment files và running digests khớp release manifest.

## Success Output

- Containers chạy đúng backend/frontend digests trong manifest.
- Database và application containers healthy.
- Public HTTPS and API smoke checks pass.
- Release manifest được lưu với trạng thái `HEALTHY`.
- Previous healthy manifest vẫn còn để rollback.

## Failure Output

- Diagnostics giới hạn được thu thập nhưng không chứa secret.
- Nếu production chưa bị đổi: trạng thái `BLOCKED`.
- Nếu production đã bị đổi và previous release tương thích: rollback rồi `ROLLED_BACK`.
- Nếu rollback không an toàn hoặc thất bại: dừng automation ở `INCIDENT`.
- Không tự động downgrade migration hoặc restore database.
- Registry/SSH/public-path failures được retry tối đa 3 lần với bounded timeout; lỗi trước production mutation là `BLOCKED`, lỗi sau mutation đi qua rollback hoặc `INCIDENT`.

## Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Deployment healthy |
| 10 | Invalid input or missing prerequisite |
| 20 | Backup gate failed |
| 30 | Deployment failed before verification |
| 40 | Health or smoke verification failed |
| 50 | Application rollback failed; incident required |
