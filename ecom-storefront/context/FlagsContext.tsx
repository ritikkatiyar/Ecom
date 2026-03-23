"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { getFrontendFlags, type FrontendFlags } from "@/lib/api/flags";

const defaultFlags: FrontendFlags = {
  betaBannerEnabled: true,
};

const FlagsContext = createContext<FrontendFlags>(defaultFlags);

export function FlagsProvider({ children }: { children: ReactNode }) {
  const [flags, setFlags] = useState<FrontendFlags>(defaultFlags);

  useEffect(() => {
    getFrontendFlags().then(setFlags);
  }, []);

  return (
    <FlagsContext.Provider value={flags}>{children}</FlagsContext.Provider>
  );
}

export function useFlags() {
  return useContext(FlagsContext);
}
