"use client";

import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createProduct } from "@/lib/api/products";
import type { ProductRequest } from "@/lib/types/product";
import { ProductForm } from "@/components/admin/ProductForm";

export default function AdminNewProductPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const create = useMutation({
    mutationFn: createProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-products"] });
      router.push("/admin/products");
    },
  });

  function handleSubmit(data: ProductRequest) {
    create.mutate(data);
  }

  return (
    <div>
      <h1 className="font-display text-4xl font-bold text-slate-900 mb-8">
        Add Product
      </h1>
      <ProductForm
        onSubmit={handleSubmit}
        isSubmitting={create.isPending}
        error={create.error instanceof Error ? create.error.message : undefined}
      />
    </div>
  );
}
