/** أنواع مشتركة بين الخادم والعميل — مطابقة لكيانات تطبيق Kotlin. */

export type CylinderStatus = "AVAILABLE" | "SOLD";
export type SaleStatus = "PAID" | "CREDIT";
export type ReportPeriod = "ALL" | "MONTH" | "WEEK" | "DAY";

export interface Customer {
  id: string;
  name: string;
  phone: string;
  totalDebt: number;
  totalPaid: number;
  createdAt: number;
}

export interface Sale {
  id: string;
  customerId: string;
  customerName: string;
  cylinderIdsJson: string;
  unitsSold: number;
  pricePerUnit: number;
  totalAmount: number;
  amountPaid: number;
  status: SaleStatus;
  saleDate: number;
  notes: string;
}

export interface Payment {
  id: string;
  customerId: string;
  customerName: string;
  amount: number;
  paymentDate: number;
  notes: string;
}

export interface StationPurchase {
  id: string;
  units: number;
  costPerUnit: number;
  totalAmount: number;
  amountPaid: number;
  purchaseDate: number;
  notes: string;
}

export interface StationPayment {
  id: string;
  amount: number;
  paymentDate: number;
  notes: string;
}

export interface CustomerDetail {
  customer: Customer | null;
  sales: Sale[];
  payments: Payment[];
  totalBought: number;
  totalPaid: number;
  /** الرصيد المتبقي (قد يكون سالباً = رصيد دائن لصالح الزبون بعد إصلاح الفحص 7). */
  balance: number;
}

export interface Stats {
  availableCount: number;
  soldCount: number;
  totalSales: number;
  totalPaid: number;
  totalCredit: number;
  totalCost: number;
  profit: number;
  stationBalance: number;
  allTimeSales: number;
  allTimePaid: number;
}

export interface AppState {
  stats: Stats;
  recentSales: Sale[];
  topDebtors: (Customer & { balance: number })[];
  customers: Customer[];
  defaultPrice: number;
  period: ReportPeriod;
}

export interface StationData {
  purchases: StationPurchase[];
  payments: StationPayment[];
  totalPurchases: number;
  balance: number;
}

export interface PinStatus {
  isSet: boolean;
  lockRemainingMs: number;
}

export const balanceOf = (c: Customer): number => c.totalDebt - c.totalPaid;
