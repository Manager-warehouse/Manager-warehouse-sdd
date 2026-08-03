package com.wms.service.user_context;


import com.wms.entity.access_control.User;

/** Interface lấy thông tin user đang đăng nhập từ SecurityContext (Spec 001). */
public interface CurrentUserService {
    User getRequiredCurrentUser();
}
