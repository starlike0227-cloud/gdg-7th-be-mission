쇼핑몰 API 명세서

## 공통 규칙

- 기본 경로: `/api/v1`
- 개발 주소 예시: `http://localhost:8080`
- JSON 필드명은 camelCase를 사용한다.
- JSON 요청에는 `Content-Type: application/json`을 사용한다. 응답 본문이 있으면 서버도 해당 헤더를 반환한다.
- ID는 1 이상의 정수이다. 가격·금액은 원(KRW) 단위의 정수이다.
- 재고는 0 이상의 정수, 구매 수량과 추가 수량은 1 이상의 정수이다. 숫자를 문자열로 보내지 않는다.
- 서버는 입력값과 계산 결과가 구현에서 지원하는 정수 범위를 넘으면 `400 INVALID_INPUT`으로 처리한다. 프론트엔드와 연동할 JSON 정수는 JavaScript의 안전한 정수 범위 이내로 제한한다.
- 성공 응답과 오류 응답의 JSON 구조는 아래 예시를 따른다.

### 이번 명세에서 정한 정책

기능 명세에 없는 세부 사항은 아래와 같이 제안한다. 실제 연동 전에 팀에서 이 정책을 함께 확정한다.

- 상품명은 앞뒤 공백을 제거한 뒤 대소문자를 구분하여 정확히 일치하는지 비교한다. 중복 검사와 조회에도 같은 규칙을 적용한다. 빈 이름은 허용하지 않는다.
- 가격은 1 이상의 정수로 제한한다. 무료 상품을 허용하려면 이 조건을 변경한다.
- 소비자 API는 인증된 USER, 관리자 API는 인증된 ADMIN이 사용한다. 인증 방식은 Bearer 토큰으로 가정하며, 토큰 발급 API는 이번 범위에서 제외한다.
- 관리자 경로를 사용한다는 이유만으로 접근을 허용하지 않는다. 서버가 토큰과 역할을 검증한다.
- 여러 상품 구매와 일괄 삭제는 전체 성공 또는 전체 실패로 처리한다. 오류 시 일부만 구매하거나 삭제하지 않는다.
- 예시 응답은 각각 독립된 상황이다. 앞선 예시의 재고 변경이 다음 예시에 누적되는 것은 아니다.

### 인증 헤더

```http
Authorization: Bearer <accessToken>
```

인증 정보가 없거나 유효하지 않으면 `401 UNAUTHORIZED`, 필요한 역할이 없으면 `403 FORBIDDEN`을 반환한다. 401 응답에는 `WWW-Authenticate: Bearer` 헤더를 포함한다.

## API 목록

| 역할 | 기능 | Method | URL |
|---|---|---|---|
| USER | 상품명으로 상품 조회 | GET | `/api/v1/products?name={상품명}` |
| USER | 여러 상품 구매 | POST | `/api/v1/purchases` |
| ADMIN | 상품 등록 | POST | `/api/v1/admin/products` |
| ADMIN | 재고 추가 | PATCH | `/api/v1/admin/products/{productId}/stock` |
| ADMIN | 상품 일괄 삭제 | DELETE | `/api/v1/admin/products?ids={ID}&ids={ID}` |

## 1. 소비자: 상품 조회

동일한 상품명은 하나만 존재하므로, 조회 성공 시 배열이 아닌 상품 객체 하나를 반환한다.

- **Method:** `GET`
- **URL:** `/api/v1/products?name={상품명}`
- **권한:** USER
- **성공 상태:** `200 OK`

### Input: Query Parameter

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| name | string | O | 정확히 조회할 상품명 |

```http
GET /api/v1/products?name=keyboard
```

한글·공백·특수문자가 있는 상품명은 URL 인코딩한다. Request Body는 없다.

### Output JSON

```json
{
  "productId": 1,
  "name": "keyboard",
  "price": 30000,
  "stockQuantity": 20
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| productId | integer | 구매 요청에 사용할 상품 ID |
| name | string | 상품명 |
| price | integer | 개당 가격(원) |
| stockQuantity | integer | 현재 재고 |

### 오류

| 상태 | code | 상황 |
|---|---|---|
| 400 | INVALID_INPUT | name 누락 또는 공백만 입력 |
| 404 | PRODUCT_NOT_FOUND | 정확히 일치하는 상품이 없음 |

## 2. 소비자: 상품 구매

하나 이상의 상품을 ID와 수량으로 구매한다. 조회 API를 먼저 호출할 필요는 없다. 구매 시점의 상품 가격과 재고를 서버에서 다시 확인한다.

- **Method:** `POST`
- **URL:** `/api/v1/purchases`
- **권한:** USER
- **성공 상태:** `201 Created`

### Input JSON

```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 3
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| items | array | O | 1개 이상의 구매 항목 |
| items[].productId | integer | O | 구매할 상품 ID |
| items[].quantity | integer | O | 구매 수량, 1 이상 |

### Output JSON

예시: keyboard는 개당 30,000원, mouse는 개당 10,000원이다.

```json
{
  "totalAmount": 90000,
  "items": [
    {
      "name": "keyboard",
      "quantity": 2,
      "amount": 60000
    },
    {
      "name": "mouse",
      "quantity": 3,
      "amount": 30000
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| totalAmount | integer | 모든 항목의 amount 합계(원) |
| items | array | 구매한 상품 목록, 요청 순서 유지 |
| items[].name | string | 구매한 상품명 |
| items[].quantity | integer | 실제 구매 수량 |
| items[].amount | integer | 구매 시점의 개당 가격 × 수량(원) |

### 처리 규칙

- 클라이언트는 가격과 총액을 보내지 않는다. 서버가 저장된 가격으로 금액을 계산한다.
- 같은 productId가 items에 중복되면 `400 DUPLICATE_PRODUCT_ID`를 반환한다.
- 모든 상품의 존재 여부와 재고를 확인한 뒤 구매 기록 생성과 재고 차감을 하나의 트랜잭션으로 처리한다.
- 하나라도 상품이 없거나 재고가 부족하면 전체 요청을 실패시키며, 구매 기록과 재고 모두 변경하지 않는다.
- 동시에 구매 요청이 들어와도 재고가 음수가 되지 않도록 서버가 동시성을 제어한다.
- 실제 결제 수단과 외부 결제 연동은 이번 명세의 범위에 포함하지 않는다.

### 오류

| 상태 | code | 상황 |
|---|---|---|
| 400 | INVALID_INPUT | items 누락·빈 배열, 잘못된 ID·수량·타입 |
| 400 | DUPLICATE_PRODUCT_ID | 동일 상품 ID 중복 |
| 404 | PRODUCT_NOT_FOUND | 상품이 하나 이상 존재하지 않음 |
| 409 | INSUFFICIENT_STOCK | 상품이 하나 이상 재고 부족 |

재고 부족 예시:

```json
{
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "요청한 수량보다 상품 재고가 부족합니다.",
    "details": {
      "productId": 1,
      "requestedQuantity": 2,
      "availableQuantity": 1
    }
  }
}
```

## 3. 관리자: 상품 등록

- **Method:** `POST`
- **URL:** `/api/v1/admin/products`
- **권한:** ADMIN
- **성공 상태:** `201 Created`

### Input JSON

```json
{
  "name": "keyboard",
  "price": 30000,
  "stockQuantity": 20
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| name | string | O | 중복되지 않는 상품명 |
| price | integer | O | 개당 가격(원), 1 이상 |
| stockQuantity | integer | O | 초기 재고, 0 이상 |

### Output JSON

등록된 ID를 이후 관리에 사용할 수 있도록 다음 응답을 제안한다.

```json
{
  "productId": 1,
  "name": "keyboard",
  "price": 30000,
  "stockQuantity": 20
}
```

필드 의미는 상품 조회 응답과 동일하다. ID는 서버가 생성하며 클라이언트가 지정하지 않는다.

### 오류

| 상태 | code | 상황 |
|---|---|---|
| 400 | INVALID_INPUT | 필수 필드 누락, 빈 이름, 잘못된 가격·재고 |
| 409 | DUPLICATE_PRODUCT_NAME | 같은 이름의 상품이 이미 존재 |

동시에 같은 이름으로 등록하더라도 하나만 등록되도록 서버의 저장 단계에서도 고유성을 보장한다.

중복 이름 예시:

```json
{
  "error": {
    "code": "DUPLICATE_PRODUCT_NAME",
    "message": "이미 등록된 상품명입니다.",
    "details": {
      "name": "keyboard"
    }
  }
}
```

## 4. 관리자: 재고 추가

- **Method:** `PATCH`
- **URL:** `/api/v1/admin/products/{productId}/stock`
- **권한:** ADMIN
- **성공 상태:** `200 OK`

### Input: Path Variable

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| productId | integer | O | 재고를 추가할 상품 ID |

```http
PATCH /api/v1/admin/products/1/stock
```

### Input JSON

```json
{
  "quantityToAdd": 10
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| quantityToAdd | integer | O | 추가할 재고, 1 이상 |

### Output JSON

기존 재고가 20개일 때의 예시이다.

```json
{
  "name": "keyboard",
  "stockQuantity": 30
}
```

- name: 상품명(string)
- stockQuantity: 추가 처리 후 최종 재고(integer)

### 처리 규칙

- 최종 재고는 `기존 재고 + quantityToAdd`이다. 요청값으로 덮어쓰지 않는다.
- 구매나 다른 재고 추가와 동시에 실행되어도 증가·감소 내역이 유실되지 않도록 원자적으로 반영한다.
- 이 API는 증가 연산이므로 같은 요청을 두 번 성공시키면 두 번 증가한다. 응답 유실 시 무조건 자동 재시도하지 않는다. 재시도를 보장해야 하는 서비스라면 별도의 중복 요청 방지 계약을 추가한다.

### 오류

| 상태 | code | 상황 |
|---|---|---|
| 400 | INVALID_INPUT | ID 또는 추가 수량이 잘못됨 |
| 404 | PRODUCT_NOT_FOUND | 해당 상품이 없음 |

## 5. 관리자: 상품 삭제

상품 ID 목록을 받아 한 번에 여러 상품을 삭제한다. 하나만 삭제하려면 ids를 한 번 전달한다.

- **Method:** `DELETE`
- **URL:** `/api/v1/admin/products?ids=1&ids=2`
- **권한:** ADMIN
- **성공 상태:** `200 OK`

### Input: Query Parameter

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| ids | integer 배열 | O | 같은 키를 반복하여 전달하는 상품 ID 목록, 1개 이상 |

여러 상품 삭제:

```http
DELETE /api/v1/admin/products?ids=1&ids=2
```

한 상품 삭제:

```http
DELETE /api/v1/admin/products?ids=1
```

Request Body는 없다. 프론트엔드는 `ids=1&ids=2`처럼 직렬화하며, `ids[]=1` 또는 쉼표 구분 방식은 사용하지 않는다.

### Output JSON

1번·2번 상품을 삭제한 뒤 3번 상품이 남은 경우이다.

```json
{
  "items": [
    {
      "name": "monitor",
      "stockQuantity": 5
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| items | array | 삭제 처리 시점에 남아 있는 전체 상품 목록, 상품 ID 오름차순 |
| items[].name | string | 상품명 |
| items[].stockQuantity | integer | 남은 재고 |

상품이 하나도 남지 않았다면:

```json
{
  "items": []
}
```

### 처리 규칙

- 중복된 ID는 하나로 처리한다.
- 대상 상품이 하나라도 존재하지 않으면 `404 PRODUCT_NOT_FOUND`를 반환하고 어떤 상품도 삭제하지 않는다.
- 성공 시 목록에 포함된 상품을 모두 삭제한다. 삭제와 남은 목록 조회를 일관된 처리 단위로 수행한다.
- 구매 기록이 있다면 당시 상품명과 가격 등 구매 이력을 보존한다. 상품 삭제가 과거 구매 기록까지 삭제한다는 뜻은 아니다.
- 남은 상품 목록을 응답해야 하므로 본문이 없는 204 대신 200을 사용한다.

### 오류

| 상태 | code | 상황 |
|---|---|---|
| 400 | INVALID_INPUT | ids 누락, 빈 값, 정수가 아니거나 1 미만인 ID |
| 404 | PRODUCT_NOT_FOUND | 삭제 대상 중 존재하지 않는 ID가 있음 |

```json
{
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "삭제할 상품 중 존재하지 않는 상품이 있습니다.",
    "details": {
      "productIds": [2]
    }
  }
}
```

## 공통 오류 응답

```json
{
  "error": {
    "code": "INVALID_INPUT",
    "message": "요청 데이터를 확인해주세요.",
    "details": {}
  }
}
```

- error.code: 클라이언트가 분기 처리할 고정 문자열이다.
- error.message: 사용자 또는 개발자가 이해할 설명이다. 분기 기준으로 사용하지 않는다.
- error.details: 추가 정보 객체이다. 추가 내용이 없으면 빈 객체를 반환한다.
- 예상하지 못한 서버 오류는 `500 INTERNAL_SERVER_ERROR`로 반환한다. 내부 SQL·스택 추적은 응답에 포함하지 않는다.

## 설계 이유

1. 조회 대상 상품명은 검색 조건이므로 Query Parameter로 표현한다. 상품의 고유 식별자인 ID는 재고 관리 경로와 구매 요청에 사용한다.
2. 여러 상품 구매는 구매라는 처리 결과를 생성하므로 `POST /purchases`로 표현한다. 금액과 재고 검증은 서버가 담당한다.
3. 여러 상품을 삭제하는 DELETE 요청은 Request Body 대신 반복 Query Parameter를 사용한다. DELETE 본문의 의미는 HTTP 표준에서 일반적으로 정의되지 않아 클라이언트·중간 장비와의 호환성을 고려한 선택이다.
4. 상품 조회와 관리 API를 구분하되, 실제 권한은 서버가 검증한다.
5. 미정인 정책을 명시해 프론트엔드와 백엔드가 요청 형식, 상태 코드, 실패 시 동작을 동일하게 이해하도록 한다.

HTTP 참고: [RFC 9110 — DELETE](https://www.rfc-editor.org/rfc/rfc9110.html#section-9.3.5)
