import type { Metadata } from "next";

export const siteName = "Anaya Candles";
export const siteDescription =
  "Handcrafted candles, home fragrances, and modern scent rituals for everyday spaces.";
export const siteUrl =
  process.env.NEXT_PUBLIC_SITE_URL?.trim() || "https://anaya-candles.vercel.app";

export function absoluteUrl(path = "/"): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${siteUrl}${normalizedPath === "/" ? "" : normalizedPath}`;
}

export function buildMetadata({
  title,
  description = siteDescription,
  path = "/",
  image,
  noIndex = false,
}: {
  title?: string;
  description?: string;
  path?: string;
  image?: string;
  noIndex?: boolean;
} = {}): Metadata {
  const resolvedTitle = title || siteName;
  const canonical = absoluteUrl(path);
  const openGraphImages = image
    ? [
        {
          url: image,
          alt: resolvedTitle,
        },
      ]
    : undefined;

  return {
    metadataBase: new URL(siteUrl),
    title:
      resolvedTitle === siteName
        ? {
            default: siteName,
            template: `%s | ${siteName}`,
          }
        : resolvedTitle,
    description,
    alternates: {
      canonical,
    },
    openGraph: {
      title: resolvedTitle,
      description,
      url: canonical,
      siteName,
      locale: "en_US",
      type: "website",
      images: openGraphImages,
    },
    twitter: {
      card: image ? "summary_large_image" : "summary",
      title: resolvedTitle,
      description,
      images: image ? [image] : undefined,
    },
    robots: noIndex
      ? {
          index: false,
          follow: false,
          googleBot: {
            index: false,
            follow: false,
          },
        }
      : undefined,
  };
}
