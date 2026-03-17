import type { Metadata } from "next";
import { Inter, Newsreader } from "next/font/google";
import { AuthProvider } from "@/context/AuthContext";
import { FlagsProvider } from "@/context/FlagsContext";
import { Analytics } from "@/components/Analytics";
import { BetaBanner } from "@/components/BetaBanner";
import { BetaOnboardingModal } from "@/components/BetaOnboardingModal";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { QueryProvider } from "@/providers/QueryProvider";
import "./globals.css";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
});

const newsreader = Newsreader({
  variable: "--font-newsreader",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Anaya Candles",
  description: "Handcrafted candles for your space",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="light">
      <head>
        <link
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL@24,400,0&display=swap"
          rel="stylesheet"
        />
      </head>
      <body
        className={`${inter.variable} ${newsreader.variable} antialiased bg-background text-slate-900`}
      >
        <Analytics />
        <QueryProvider>
          <FlagsProvider>
            <AuthProvider>
              <BetaBanner />
              <BetaOnboardingModal />
              <Header />
              {children}
              <Footer />
            </AuthProvider>
          </FlagsProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
