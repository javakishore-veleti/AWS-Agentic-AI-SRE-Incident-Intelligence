export type ProductStatus = "ACTIVE" | "DISCONTINUED"
export type OfferStatus = "DRAFT" | "ACTIVE" | "EXPIRED"
export type CustomerStatus = "ACTIVE" | "SUSPENDED"
export type AccountRole = "CUSTOMER" | "ADMIN"
export type OrderStatus = "RESERVING" | "RESERVATION_FAILED" | "PLACED" | "CONFIRMED" | "FULFILLED" | "CANCELLED"

export type Product = {
  id: string
  sku: string
  name: string
  description: string
  category: string
  unitPrice: number
  status: ProductStatus
  createdAt: string
  updatedAt: string
}

export type ProductSummary = {
  id: string
  sku: string
  name: string
  category: string
  unitPrice: number
}

export type Offer = {
  id: string
  code: string
  name: string
  description: string
  offerPrice: number
  status: OfferStatus
  startsOn: string | null
  endsOn: string | null
  productIds: string[]
  products: ProductSummary[]
  createdAt: string
  updatedAt: string
}

export type Customer = {
  id: string
  fullName: string
  email: string
  phone: string
  status: CustomerStatus
  role: AccountRole
  createdAt: string
  updatedAt: string
}

export type OrderLine = {
  productId: string
  sku: string
  productName: string
  quantity: number
  unitPrice: number
}

export type ShopOrder = {
  id: string
  orderNumber: string
  customerId: string
  customerName: string
  customerEmail: string
  offerId: string
  offerName: string
  totalAmount: number
  status: OrderStatus
  note: string
  lines: OrderLine[]
  placedAt: string
  updatedAt: string
}
