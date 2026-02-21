"use client";

import { useRouter, useParams } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getProduct, updateProduct } from "@/lib/api/products";
import type { ProductRequest } from "@/lib/types/product";
import { ProductForm } from "@/components/admin/ProductForm";

export default function AdminEditProductPage() {
  const router = useRouter();
  const params = useParams();
  const id = params.id as string;
  const queryClient = useQueryClient();

  const { data: product, isLoading, error } = useQuery({
    queryKey: ["admin-product", id],
    queryFn: () => getProduct(id),
    enabled: !!id,
  });

  const update = useMutation({
    mutationFn: (data: ProductRequest) => updateProduct(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-products"] });
      queryClient.invalidateQueries({ queryKey: ["admin-product", id] });
      router.push("/admin/products");
    },
  });

  function handleSubmit(data: ProductRequest) {
    update.mutate(data);
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-[#2badee] border-t-transparent" />
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="rounded-lg bg-red-50 px-4 py-3 text-red-700">
        {error instanceof Error ? error.message : "Product not found"}
      </div>
    );
  }

  const initial: ProductRequest = {
    name: product.name,
    description: product.description ?? "",
    category: product.category,
    brand: product.brand,
    price: typeof product.price === "number" ? product.price : Number(product.price),
    colors: product.colors ?? [],
    sizes: product.sizes ?? [],
    active: product.active ?? true,
    imageUrls: product.imageUrls ?? [],
  };

  return (
    <div>
      <h1 className="font-display text-4xl font-bold text-slate-900 mb-8">
        Edit Product
      </h1>
      <ProductForm
        initial={initial}
        onSubmit={handleSubmit}
        isSubmitting={update.isPending}
        error={update.error instanceof Error ? update.error.message : undefined}
      />
    </div>
  );
}
