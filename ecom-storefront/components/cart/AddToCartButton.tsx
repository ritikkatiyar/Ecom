"use client";

import { useState } from "react";
import { useCart } from "@/lib/hooks/useCart";

interface AddToCartButtonProps {
  productId: string;
}

export function AddToCartButton({ productId }: AddToCartButtonProps) {
  const { addItem, isMutating } = useCart();
  const [state, setState] = useState<"idle" | "added" | "error">("idle");

  const handleAdd = async () => {
    setState("idle");
    try {
      await addItem({ productId, quantity: 1 });
      setState("added");
      setTimeout(() => setState("idle"), 1200);
    } catch {
      setState("error");
    }
  };

  return (
    <div className="w-full">
      <button
        type="button"
        disabled={isMutating}
        onClick={handleAdd}
        className="w-full bg-[#2badee] hover:bg-[#2badee]/90 text-white font-bold py-5 rounded-lg transition-all tracking-widest uppercase text-sm disabled:opacity-60"
      >
        {isMutating ? "Adding..." : "Add to Cart"}
      </button>
      {state === "added" ? (
        <p className="mt-2 text-xs uppercase tracking-widest text-emerald-600">
          Added to cart
        </p>
      ) : null}
      {state === "error" ? (
        <p className="mt-2 text-xs uppercase tracking-widest text-red-600">
          Failed to add item
        </p>
      ) : null}
    </div>
  );
}
