"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { ApiError } from "@/lib/apiClient";
import { uploadProductImages } from "@/lib/api/products";
import type { ProductRequest } from "@/lib/types/product";

interface ProductFormProps {
  initial?: ProductRequest;
  onSubmit: (data: ProductRequest) => void;
  isSubmitting?: boolean;
  error?: string;
}

export function ProductForm({
  initial,
  onSubmit,
  isSubmitting = false,
  error,
}: ProductFormProps) {
  const formRef = useRef<HTMLFormElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [imageUrls, setImageUrls] = useState<string[]>(initial?.imageUrls ?? []);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploadInfo, setUploadInfo] = useState<string | null>(null);

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const files = e.target.files;
    if (!files?.length) return;
    setUploadError(null);
    setUploadInfo(`Uploading ${files.length} image(s)...`);
    setUploading(true);
    try {
      const urls = await uploadProductImages(Array.from(files));
      setImageUrls((prev) => [...prev, ...urls]);
      setUploadInfo(`Uploaded ${urls.length} image(s) successfully.`);
    } catch (err) {
      if (err instanceof ApiError) {
        const cid = err.correlationId ? ` (Correlation ID: ${err.correlationId})` : "";
        setUploadError(`${err.message}${cid}`);
      } else {
        setUploadError(err instanceof Error ? err.message : "Upload failed");
      }
      setUploadInfo(null);
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  }

  function removeImage(url: string) {
    setImageUrls((prev) => prev.filter((u) => u !== url));
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = formRef.current;
    if (!form) return;
    const fd = new FormData(form);
    const colors =
      (fd.get("colors") as string)?.split(",").map((s) => s.trim()).filter(Boolean) ?? [];
    const sizes =
      (fd.get("sizes") as string)?.split(",").map((s) => s.trim()).filter(Boolean) ?? [];
    onSubmit({
      name: (fd.get("name") as string) ?? "",
      description: (fd.get("description") as string) || undefined,
      category: (fd.get("category") as string) ?? "",
      brand: (fd.get("brand") as string) ?? "",
      price: Number(fd.get("price")) || 0,
      active: fd.get("active") === "on",
      colors: colors.length ? colors : undefined,
      sizes: sizes.length ? sizes : undefined,
      imageUrls: imageUrls.length ? imageUrls : undefined,
    });
  }

  return (
    <form
      ref={formRef}
      onSubmit={handleSubmit}
      className="max-w-2xl space-y-6 rounded-xl border border-slate-200 bg-white p-8"
    >
      {error && (
        <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}

      {uploadError && (
        <div className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <p>{uploadError}</p>
          <p className="mt-1 text-xs">
            Retry upload. If this persists, share the correlation ID for backend tracing.
          </p>
        </div>
      )}

      {uploadInfo && (
        <div className="rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-700">
          {uploadInfo}
        </div>
      )}

      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">Images</label>
        <div className="space-y-3">
          {imageUrls.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {imageUrls.map((url) => (
                <div
                  key={url}
                  className="relative group w-20 h-20 rounded-lg overflow-hidden border border-slate-200 bg-slate-50"
                >
                  <img src={url} alt="Product" className="w-full h-full object-cover" />
                  <button
                    type="button"
                    onClick={() => removeImage(url)}
                    className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity"
                  >
                    <span className="text-white text-xs font-medium">Remove</span>
                  </button>
                </div>
              ))}
            </div>
          )}
          <div className="flex items-center gap-2">
            <input
              ref={fileInputRef}
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              multiple
              onChange={handleFileChange}
              className="hidden"
            />
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
              className="px-4 py-2 border border-dashed border-slate-300 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-50"
            >
              {uploading ? "Uploading..." : "Add images"}
            </button>
          </div>
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">Name *</label>
        <input
          name="name"
          type="text"
          required
          defaultValue={initial?.name}
          className="w-full rounded-lg border border-slate-300 px-4 py-3 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">Description</label>
        <textarea
          name="description"
          rows={3}
          defaultValue={initial?.description}
          className="w-full rounded-lg border border-slate-300 px-4 py-3 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Category *</label>
          <input
            name="category"
            type="text"
            required
            defaultValue={initial?.category}
            className="w-full rounded-lg border border-slate-300 px-4 py-3 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Brand *</label>
          <input
            name="brand"
            type="text"
            required
            defaultValue={initial?.brand}
            className="w-full rounded-lg border border-slate-300 px-4 py-3 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">Price (INR) *</label>
        <input
          name="price"
          type="number"
          step="0.01"
          min="0"
          required
          defaultValue={initial?.price ?? ""}
          className="w-full rounded-lg border border-slate-300 px-4 py-3 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">
          Colors (comma-separated)
        </label>
        <input
          name="colors"
          type="text"
          placeholder="Red, Blue, Green"
          defaultValue={initial?.colors?.join(", ")}
          className="w-full rounded-lg border border-slate-300 px-4 py-3 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">
          Sizes (comma-separated)
        </label>
        <input
          name="sizes"
          type="text"
          placeholder="S, M, L"
          defaultValue={initial?.sizes?.join(", ")}
          className="w-full rounded-lg border border-slate-300 px-4 py-3 text-slate-900 focus:border-[#2badee] focus:outline-none focus:ring-2 focus:ring-[#2badee]/20"
        />
      </div>

      <div className="flex items-center gap-2">
        <input
          name="active"
          type="checkbox"
          defaultChecked={initial?.active ?? true}
          className="rounded border-slate-300 text-[#2badee] focus:ring-[#2badee]"
        />
        <label className="text-sm font-medium text-slate-700">Active</label>
      </div>

      <div className="flex gap-4 pt-4">
        <button
          type="submit"
          disabled={isSubmitting}
          className="px-6 py-3 bg-[#2badee] text-white font-semibold rounded-lg hover:bg-[#2badee]/90 disabled:opacity-50"
        >
          {isSubmitting ? "Saving..." : "Save"}
        </button>
        <Link
          href="/admin/products"
          className="px-6 py-3 border border-slate-200 rounded-lg font-medium text-slate-700 hover:bg-slate-50"
        >
          Cancel
        </Link>
      </div>
    </form>
  );
}
