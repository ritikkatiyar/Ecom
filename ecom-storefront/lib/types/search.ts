export interface SearchProduct {
  productId: string;
  name: string;
  description?: string;
  category?: string;
  brand?: string;
  price: number;
  colors?: string[];
  sizes?: string[];
  active?: boolean;
  updatedAt?: string;
}

export interface SearchProductPage {
  content: SearchProduct[];
  totalElements: number;
  page: number;
  size: number;
}
