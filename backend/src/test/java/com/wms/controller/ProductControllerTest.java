package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.request.ProductRequest;
import com.wms.dto.response.ProductResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.UserRepository;
import com.wms.service.ProductService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ProductService productService;
    @MockBean
    UserRepository userRepository;
    @MockBean
    JwtUtil jwtUtil;
    @MockBean
    UserDetailsServiceImpl userDetailsService;

    private ProductResponse sampleResponse;
    private User storekeepUser;
    private User ceoUser;

    @BeforeEach
    void setUp() {
        sampleResponse = ProductResponse.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Sản phẩm A")
                .unit("cái")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        storekeepUser = new User();
        storekeepUser.setId(1L);
        storekeepUser.setEmail("storekeeper@wms.com");
        storekeepUser.setRole(UserRole.STOREKEEPER);

        ceoUser = new User();
        ceoUser.setId(2L);
        ceoUser.setEmail("ceo@wms.com");
        ceoUser.setRole(UserRole.CEO);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/products
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "STOREKEEPER")
    @DisplayName("GET /products - STOREKEEPER - 200 + danh sách sản phẩm")
    void getProducts_storekeeper_returns200() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(sampleResponse));
        when(productService.getProducts(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-001"));
    }

    @Test
    @WithMockUser(roles = "CEO")
    @DisplayName("GET /products - CEO - 200 (tất cả role đăng nhập đều xem được)")
    void getProducts_ceo_returns200() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(sampleResponse));
        when(productService.getProducts(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /products - không xác thực - 403")
    void getProducts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STOREKEEPER")
    @DisplayName("GET /products?search=SKU - có search param - 200")
    void getProducts_withSearch_returns200() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(sampleResponse));
        when(productService.getProducts(eq("SKU"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products").param("search", "SKU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-001"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/products/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "STOREKEEPER")
    @DisplayName("GET /products/{id} - tồn tại - 200 + chi tiết")
    void getProduct_exists_returns200() throws Exception {
        when(productService.getProduct(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @WithMockUser(roles = "STOREKEEPER")
    @DisplayName("GET /products/{id} - không tồn tại - 404")
    void getProduct_notFound_returns404() throws Exception {
        when(productService.getProduct(99L)).thenThrow(new ResourceNotFoundException("PRODUCT_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/products
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    @DisplayName("POST /products - STOREKEEPER, valid body - 201")
    void createProduct_storekeeper_returns201() throws Exception {
        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeepUser));
        when(productService.createProduct(any(), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("SKU-001"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    @WithMockUser(roles = "CEO")
    @DisplayName("POST /products - CEO - 403")
    void createProduct_ceo_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("SKU-001"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    @DisplayName("POST /products - SKU trùng - 409 DUPLICATE_SKU")
    void createProduct_duplicateSku_returns409() throws Exception {
        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeepUser));
        when(productService.createProduct(any(), any())).thenThrow(new IllegalArgumentException("DUPLICATE_SKU"));

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("SKU-001"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_SKU"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    @DisplayName("POST /products - thiếu sku - 400 validation error")
    void createProduct_missingSku_returns400() throws Exception {
        ProductRequest request = buildRequest(null);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    @DisplayName("POST /products - thiếu name - 400 validation error")
    void createProduct_missingName_returns400() throws Exception {
        ProductRequest request = buildRequest("SKU-001");
        request.setName(null);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/products/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    @DisplayName("PUT /products/{id} - STOREKEEPER, valid - 200")
    void updateProduct_storekeeper_returns200() throws Exception {
        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeepUser));
        when(productService.updateProduct(eq(1L), any(), eq(1L))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("SKU-001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("PUT /products/{id} - PLANNER - 403")
    void updateProduct_planner_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("SKU-001"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    @DisplayName("PUT /products/{id} - ID không tồn tại - 404")
    void updateProduct_notFound_returns404() throws Exception {
        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeepUser));
        when(productService.updateProduct(eq(99L), any(), any()))
                .thenThrow(new ResourceNotFoundException("PRODUCT_NOT_FOUND"));

        mockMvc.perform(put("/api/v1/products/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("SKU-X"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/products/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    @DisplayName("DELETE /products/{id} - STOREKEEPER - 204 No Content")
    void deactivateProduct_storekeeper_returns204() throws Exception {
        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeepUser));
        doNothing().when(productService).deactivateProduct(eq(1L), eq(1L));

        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    @DisplayName("DELETE /products/{id} - ADMIN - 204 No Content")
    void deactivateProduct_admin_returns204() throws Exception {
        User adminUser = new User();
        adminUser.setId(3L);
        adminUser.setEmail("admin@wms.com");
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
        doNothing().when(productService).deactivateProduct(eq(1L), eq(3L));

        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CEO")
    @DisplayName("DELETE /products/{id} - CEO - 403")
    void deactivateProduct_ceo_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    @DisplayName("DELETE /products/{id} - ID không tồn tại - 404")
    void deactivateProduct_notFound_returns404() throws Exception {
        when(userRepository.findByEmail("storekeeper@wms.com")).thenReturn(Optional.of(storekeepUser));
        doThrow(new ResourceNotFoundException("PRODUCT_NOT_FOUND"))
                .when(productService).deactivateProduct(eq(99L), any());

        mockMvc.perform(delete("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProductRequest buildRequest(String sku) {
        ProductRequest r = new ProductRequest();
        r.setSku(sku);
        r.setName("Sản phẩm Test");
        r.setUnit("cái");
        return r;
    }
}


