export interface Product {
  id: string;
  name: string;
  description?: string;
  category: string;
  brand: string;
  price: number;
  active: boolean;
  colors?: string[];
  sizes?: string[];
  imageUrls?: string[];
}

export interface ProductRequest {
  name: string;
  description?: string;
  category: string;
  brand: string;
  price: number;
  colors?: string[];
  sizes?: string[];
  active?: boolean;
  imageUrls?: string[];
}

export interface ProductPage {
  content: Product[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
