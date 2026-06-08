package com.wms.service;

import com.wms.entity.User;

public interface CurrentUserService {
    User getRequiredCurrentUser();
}
