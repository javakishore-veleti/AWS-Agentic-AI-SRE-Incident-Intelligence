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
  status: string
  startsOn: string | null
  endsOn: string | null
  productIds: string[]
  products: ProductSummary[]
  createdAt: string
}

export type Customer = {
  id: string
  fullName: string
  email: string
  phone: string
  status: string
}

export type Session = {
  token: string
  customer: Customer
}

export type ShopOrder = {
  id: string
  orderNumber: string
  offerName: string
  totalAmount: number
  status: string
  placedAt: string
  lines: { sku: string; productName: string; quantity: number }[]
}
