## 기술적 의사결정

### Controller 테스트에 RestTestClient 사용

컨트롤러 테스트에 `MockMvc` 대신 `RestTestClient`를 사용합니다.

Spring Boot 4.x는 `TestRestTemplate`을 deprecated 예고하고 `RestTestClient`를 공식 후계자로 도입했습니다.
`MockMvc`를 직접 사용하는 방식도 유지되지만, Spring 공식 가이드와 레퍼런스 문서가 이미 `RestTestClient` 기준으로 교체된 상태입니다.

`MockMvc`는 Servlet 기반 전용 API로 `perform()` · `andExpect()` 같은 독자적인 어휘를 가집니다.
반면 `RestTestClient`는 WebFlux 테스트의 `WebTestClient`와 동일한 fluent API를 공유하도록 설계되어,
Mock 서버 모드와 실서버 모드를 어노테이션 하나(`@WebMvcTest` ↔ `@SpringBootTest`)만 교체해 전환할 수 있습니다.

```java
// Before — MockMvc
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @WithMockUser
    void 주문_조회_성공() throws Exception {
        given(orderService.findById(1L)).willReturn(new OrderResponse(...));

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L));
    }
}
```

```java
// After — RestTestClient
@WebMvcTest(OrderController.class)
@AutoConfigureRestTestClient
class OrderControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private OrderService orderService;

    @Test
    @WithMockUser
    void 주문_조회_성공() {
        given(orderService.findById(1L)).willReturn(new OrderResponse(...));

        restTestClient.get().uri("/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo(1L);
    }
}
```

`@WebMvcTest`는 MVC 레이어만 로딩하는 슬라이스 구성 역할로 그대로 유지되고,
`@AutoConfigureRestTestClient`를 추가해 `RestTestClient` 빈을 주입받습니다.
전환으로 `throws Exception` 선언, `MockMvcRequestBuilders` · `MockMvcResultMatchers` 관련 static import,
그리고 Boot 4.x에서 제거된 `@MockBean`이 함께 정리됩니다.