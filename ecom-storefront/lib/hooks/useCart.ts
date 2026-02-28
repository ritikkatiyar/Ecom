"use client";

import { useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { addCartItem, clearCart, getCart, removeCartItem } from "@/lib/api/cart";
import { getOrCreateGuestId } from "@/lib/cartSession";
import { useAuth } from "@/context/AuthContext";
import type { CartResponse } from "@/lib/types/cart";

function parseUserId(userId: string | null): number | null {
  if (!userId) return null;
  const n = Number(userId);
  return Number.isFinite(n) ? n : null;
}

function emptyCart(ownerId: string): CartResponse {
  return {
    ownerType: "GUEST",
    ownerId,
    totalItems: 0,
    items: [],
  };
}

export function useCart() {
  const { userId } = useAuth();
  const queryClient = useQueryClient();

  const owner = useMemo(() => {
    const parsedUserId = parseUserId(userId);
    if (parsedUserId != null) return { userId: parsedUserId, guestId: undefined, key: `user:${parsedUserId}` };
    const guestId = getOrCreateGuestId();
    return { userId: undefined, guestId, key: `guest:${guestId}` };
  }, [userId]);

  const queryKey = useMemo(() => ["cart", owner.key], [owner.key]);

  const cartQuery = useQuery({
    queryKey,
    queryFn: () => getCart({ userId: owner.userId, guestId: owner.guestId }),
    staleTime: 10 * 1000,
  });

  const addMutation = useMutation({
    mutationFn: (args: { productId: string; quantity: number }) =>
      addCartItem({
        productId: args.productId,
        quantity: args.quantity,
        userId: owner.userId,
        guestId: owner.guestId,
      }),
    onMutate: async ({ productId, quantity }) => {
      await queryClient.cancelQueries({ queryKey });
      const prev = queryClient.getQueryData<CartResponse>(queryKey);
      const base = prev ?? emptyCart(owner.guestId ?? String(owner.userId ?? "guest"));
      const found = base.items.find((i) => i.productId === productId);
      const nextItems = found
        ? base.items.map((i) =>
            i.productId === productId ? { ...i, quantity: i.quantity + quantity } : i
          )
        : [...base.items, { productId, quantity }];
      queryClient.setQueryData<CartResponse>(queryKey, {
        ...base,
        totalItems: Math.max(0, base.totalItems + quantity),
        items: nextItems,
      });
      return { prev };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.prev) queryClient.setQueryData(queryKey, ctx.prev);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const removeMutation = useMutation({
    mutationFn: (productId: string) =>
      removeCartItem(productId, { userId: owner.userId, guestId: owner.guestId }),
    onMutate: async (productId) => {
      await queryClient.cancelQueries({ queryKey });
      const prev = queryClient.getQueryData<CartResponse>(queryKey);
      if (!prev) return { prev };
      const removed = prev.items.find((i) => i.productId === productId);
      queryClient.setQueryData<CartResponse>(queryKey, {
        ...prev,
        totalItems: Math.max(0, prev.totalItems - (removed?.quantity ?? 0)),
        items: prev.items.filter((i) => i.productId !== productId),
      });
      return { prev };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.prev) queryClient.setQueryData(queryKey, ctx.prev);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  const clearMutation = useMutation({
    mutationFn: () => clearCart({ userId: owner.userId, guestId: owner.guestId }),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey });
      const prev = queryClient.getQueryData<CartResponse>(queryKey);
      queryClient.setQueryData<CartResponse>(queryKey, emptyCart(owner.guestId ?? String(owner.userId ?? "guest")));
      return { prev };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.prev) queryClient.setQueryData(queryKey, ctx.prev);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey });
    },
  });

  return {
    cart: cartQuery.data,
    isLoading: cartQuery.isLoading,
    isFetching: cartQuery.isFetching,
    error: cartQuery.error,
    addItem: addMutation.mutateAsync,
    removeItem: removeMutation.mutateAsync,
    clear: clearMutation.mutateAsync,
    isMutating:
      addMutation.isPending || removeMutation.isPending || clearMutation.isPending,
  };
}
