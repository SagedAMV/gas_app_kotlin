import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";
import { Cairo, IBM_Plex_Sans_Arabic } from "next/font/google";
import "./globals.css";

const plex = IBM_Plex_Sans_Arabic({
  subsets: ["arabic", "latin"],
  weight: ["300", "400", "500", "600", "700"],
  variable: "--font-plex-arabic",
});

const cairo = Cairo({
  subsets: ["arabic", "latin"],
  weight: ["500", "700", "800", "900"],
  variable: "--font-cairo",
});

export const metadata: Metadata = {
  title: "دَبّ لتجارة الغاز",
  description: "نظام إدارة مخزون وديون تجارة أسطوانات الغاز — إعادة بناء ويب لتطبيق Kotlin الشخصي.",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  themeColor: "#23201c",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ar" dir="rtl" className={`${plex.variable} ${cairo.variable}`}>
      <body className="min-h-dvh bg-paper text-ink antialiased">{children}</body>
    </html>
  );
}
