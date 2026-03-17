"use client";

import { useState } from "react";
import { useCart } from "@/lib/hooks/useCart";

interface AddToCartButtonProps {
  productId: string;
  variant?: "default" | "compact";
}

export function AddToCartButton({ productId, variant = "default" }: AddToCartButtonProps) {
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

  const isCompact = variant === "compact";

  return (
    <div className={isCompact ? "" : "w-full"}>
      <button
        type="button"
        disabled={isMutating}
        onClick={handleAdd}
        className={
          isCompact
            ? "inline-flex items-center gap-1.5 rounded-lg border border-[#2badee] bg-[#2badee]/5 px-3 py-2 text-xs font-semibold uppercase tracking-widest text-[#2badee] hover:bg-[#2badee]/15 disabled:opacity-60"
            : "w-full bg-[#2badee] hover:bg-[#2badee]/90 text-white font-bold py-5 rounded-lg transition-all tracking-widest uppercase text-sm disabled:opacity-60"
        }
      >
        {isMutating ? "Adding..." : "Add to Cart"}
      </button>
      {state === "added" && !isCompact ? (
        <p className="mt-2 text-xs uppercase tracking-widest text-emerald-600">
          Added to cart
        </p>
      ) : null}
      {state === "error" && !isCompact ? (
        <p className="mt-2 text-xs uppercase tracking-widest text-red-600">
          Failed to add item
        </p>
      ) : null}
    </div>
  );
}
