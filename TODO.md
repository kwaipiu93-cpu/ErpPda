# ERP PDA App — 需求追蹤

> 最後更新：2026-06-08  
> 格式：✅ 已完成 / 🔧 進行中 / 📋 待做

---

## ✅ 已完成

### 架構
- [x] iOS 風格主題（配色 #007AFF、SF 字體、大圓角）
- [x] 底部 5 Tab 導航（首頁/採購/銷售/倉庫/更多）
- [x] 隱藏系統導航欄（全屏沉浸式）
- [x] 所有列表支援 Pull-to-Refresh
- [x] 所有子頁面左上角 iOS 返回掣
- [x] 登入保留（token + 帳密自動登入）

### 銷售模組
- [x] 報價查詢（列表 + 右下角 ⊕ FAB 建立新報價）
- [x] 報價詳情頁（查看 + 編輯模式）
- [x] 報價編輯：改客戶、倉庫、備註、增刪改項目
- [x] 編輯項目彈窗（數量、單價、描述）
- [x] 報價 PDF 下載（存到 cache）
- [x] 發票列表（全部/未付/已付篩選）
- [x] 快速結帳（B2C 4 步流程）
- [x] 客戶管理（列表 + 詳情）
- [x] 收款記錄

### 採購模組
- [x] 採購單列表（全部/待收貨/已收貨篩選 + ⊕ FAB）
- [x] 建立採購單
- [x] 採購收貨（PDA 掃描 S/N）
- [x] S/N 保固查詢（雙軌保固穿透）

### 倉庫模組
- [x] 出貨確認
- [x] 庫存盤點
- [x] 快速查庫存
- [x] 退貨驗收
- [x] 跨倉調撥

### Backend 修正
- [x] 修復 update_invoice 漏 INSERT
- [x] 修復 Gson parse: monetary fields String→Double
- [x] 所有 API endpoint 正確定義

---

## 📋 待做 / 未來需求

### UX 改善
- [ ] PDA 震動反饋驗證（收到 scan 要有 feel）
- [ ] 報價單列表可篩選狀態（草稿/已接受）
- [ ] 發票列表點擊跳轉發票詳情（目前用舊 InvoiceLookupScreen）
- [ ] 採購單列表點擊跳轉採購詳情 + 可編輯
- [ ] 離線模式（無網絡時 queue 操作）

### 功能
- [ ] 報價單發送（send quotation → email/WhatsApp）
- [ ] 發票 PDF 下載
- [ ] 供應商管理頁面
- [ ] Dashboard 首頁即時數據（今日訂單數、待收貨數等）
- [ ] 通知/提醒（欠單、過期報價、逾期發票）

### 技術
- [ ] 補齊單元測試
- [ ] CI/CD pipeline
- [ ] APK 簽署 release build

---

## 🔧 已知問題
- [ ] PDA 鍵盤彈出時可能遮擋輸入框
- [ ] StockCheckScreen / StocktakeScreen 部分 status badge 顏色未完全 iOS 化

---

_此檔案由 Bill2 自動維護，可手動編輯。_
