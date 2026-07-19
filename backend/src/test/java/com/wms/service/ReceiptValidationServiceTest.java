package com.wms.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceiptValidationServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;

    private ReceiptValidationService service;

    @BeforeEach
    void setUp() {
        service = new ReceiptValidationService(receiptRepository, userWarehouseAssignmentRepository);
    }

    private User user(UserRole role) {
        User user = new User();
        user.setId(1L);
        user.setRole(role);
        return user;
    }

    @Test
    void assertWarehouseAccess_allowsAccountantWithoutWarehouseAssignment() {
        assertDoesNotThrow(() -> service.assertWarehouseAccess(user(UserRole.ACCOUNTANT), 20L));
    }

    @Test
    void assertWarehouseAccess_allowsAccountantManagerWithoutWarehouseAssignment() {
        assertDoesNotThrow(() -> service.assertWarehouseAccess(user(UserRole.ACCOUNTANT_MANAGER), 20L));
    }

    @Test
    void assertWarehouseAccess_rejectsUnassignedWarehouseStaff() {
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(99L));

        assertThrows(ForbiddenReceiptWarehouseException.class,
                () -> service.assertWarehouseAccess(user(UserRole.WAREHOUSE_STAFF), 20L));
    }

    @Test
    void assertWarehouseAccess_allowsAssignedWarehouseStaff() {
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));

        assertDoesNotThrow(() -> service.assertWarehouseAccess(user(UserRole.WAREHOUSE_STAFF), 20L));
    }
}
