-- Date-range report HTML templates (company header filled from COMPANY_* settings at render time).

INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES
('b1f1c2a0-1111-4a11-9a01-00000000d1a1', 'PRODUCTION_REPORT_TEMPLATE', '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 28px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #0F172A; font-size: 11px; }
  .company { font-size: 18px; font-weight: bold; }
  .muted { color: #64748B; font-size: 10px; }
  .title { font-size: 15px; font-weight: bold; margin-top: 12px; }
  .period { color: #475569; font-size: 11px; margin-top: 2px; }
  hr { border: none; border-top: 1px solid #E2E8F0; margin: 10px 0; }
  table { width: 100%; border-collapse: collapse; margin-top: 6px; }
  th { background: #F1F5F9; text-align: left; padding: 6px 8px; font-size: 10px; color: #475569; border-bottom: 1px solid #E2E8F0; }
  td { padding: 6px 8px; border-bottom: 1px solid #EEF2F6; }
  .num { text-align: right; }
  .cards td { border: 1px solid #E2E8F0; background: #F8FAFC; padding: 10px; width: 33%; }
  .clabel { color: #64748B; font-size: 10px; }
  .cvalue { font-size: 16px; font-weight: bold; }
  .sec { font-size: 12px; font-weight: bold; margin-top: 16px; }
  .foot { margin-top: 18px; color: #94A3B8; font-size: 9px; text-align: center; }
</style>
</head>
<body>
  <div class="company">{{companyName}}</div>
  <div class="muted">{{companyAddress}} | {{companyContact}} | GSTIN {{companyGstin}}</div>
  <div class="title">Production report</div>
  <div class="period">Period: {{fromDate}} to {{toDate}}</div>
  <hr/>
  <table class="cards">
    <tr>
      <td><div class="clabel">Entries</div><div class="cvalue">{{totalEntries}}</div></td>
      <td><div class="clabel">Total pieces</div><div class="cvalue">{{totalQuantity}}</div></td>
      <td><div class="clabel">Total payment</div><div class="cvalue">{{totalAmount}}</div></td>
    </tr>
  </table>
  <div class="sec">By product</div>
  <table>
    <thead><tr><th>Product</th><th class="num">Pieces</th><th class="num">Payment</th></tr></thead>
    <tbody>{{productRows}}</tbody>
  </table>
  <div class="sec">By employee</div>
  <table>
    <thead><tr><th>Employee</th><th class="num">Pieces</th><th class="num">Payment</th></tr></thead>
    <tbody>{{employeeRows}}</tbody>
  </table>
  <div class="foot">Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for the production date-range report PDF', now(), now());

INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES
('b2f2c2a0-2222-4a22-9a02-00000000d2a2', 'SALES_REPORT_TEMPLATE', '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 28px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #0F172A; font-size: 11px; }
  .company { font-size: 18px; font-weight: bold; }
  .muted { color: #64748B; font-size: 10px; }
  .title { font-size: 15px; font-weight: bold; margin-top: 12px; }
  .period { color: #475569; font-size: 11px; margin-top: 2px; }
  hr { border: none; border-top: 1px solid #E2E8F0; margin: 10px 0; }
  table { width: 100%; border-collapse: collapse; margin-top: 6px; }
  th { background: #F1F5F9; text-align: left; padding: 6px 8px; font-size: 10px; color: #475569; border-bottom: 1px solid #E2E8F0; }
  td { padding: 6px 8px; border-bottom: 1px solid #EEF2F6; }
  .num { text-align: right; }
  .cards td { border: 1px solid #E2E8F0; background: #F8FAFC; padding: 10px; width: 20%; }
  .clabel { color: #64748B; font-size: 10px; }
  .cvalue { font-size: 15px; font-weight: bold; }
  .sec { font-size: 12px; font-weight: bold; margin-top: 16px; }
  .foot { margin-top: 18px; color: #94A3B8; font-size: 9px; text-align: center; }
</style>
</head>
<body>
  <div class="company">{{companyName}}</div>
  <div class="muted">{{companyAddress}} | {{companyContact}} | GSTIN {{companyGstin}}</div>
  <div class="title">Sales report</div>
  <div class="period">Period: {{fromDate}} to {{toDate}}</div>
  <hr/>
  <table class="cards">
    <tr>
      <td><div class="clabel">Sales</div><div class="cvalue">{{totalSales}}</div></td>
      <td><div class="clabel">Revenue</div><div class="cvalue">{{totalRevenue}}</div></td>
      <td><div class="clabel">Discount</div><div class="cvalue">{{totalDiscount}}</div></td>
      <td><div class="clabel">GST</div><div class="cvalue">{{totalTax}}</div></td>
      <td><div class="clabel">Profit</div><div class="cvalue">{{totalProfit}}</div></td>
    </tr>
  </table>
  <div class="sec">By product</div>
  <table>
    <thead><tr><th>Product</th><th class="num">Qty sold</th><th class="num">Revenue</th><th class="num">Profit</th></tr></thead>
    <tbody>{{productRows}}</tbody>
  </table>
  <div class="foot">Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for the sales date-range report PDF', now(), now());

INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES
('b3f3c2a0-3333-4a33-9a03-00000000d3a3', 'EMPLOYEE_REPORT_TEMPLATE', '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 28px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #0F172A; font-size: 11px; }
  .company { font-size: 18px; font-weight: bold; }
  .muted { color: #64748B; font-size: 10px; }
  .title { font-size: 15px; font-weight: bold; margin-top: 12px; }
  .period { color: #475569; font-size: 11px; margin-top: 2px; }
  hr { border: none; border-top: 1px solid #E2E8F0; margin: 10px 0; }
  table { width: 100%; border-collapse: collapse; margin-top: 6px; }
  th { background: #F1F5F9; text-align: left; padding: 6px 8px; font-size: 10px; color: #475569; border-bottom: 1px solid #E2E8F0; }
  td { padding: 6px 8px; border-bottom: 1px solid #EEF2F6; }
  .num { text-align: right; }
  .cards td { border: 1px solid #E2E8F0; background: #F8FAFC; padding: 10px; width: 33%; }
  .clabel { color: #64748B; font-size: 10px; }
  .cvalue { font-size: 16px; font-weight: bold; }
  .sec { font-size: 12px; font-weight: bold; margin-top: 16px; }
  .foot { margin-top: 18px; color: #94A3B8; font-size: 9px; text-align: center; }
</style>
</head>
<body>
  <div class="company">{{companyName}}</div>
  <div class="muted">{{companyAddress}} | {{companyContact}} | GSTIN {{companyGstin}}</div>
  <div class="title">Employee report - {{employeeName}} ({{employeeCode}})</div>
  <div class="period">Period: {{fromDate}} to {{toDate}}</div>
  <hr/>
  <table class="cards">
    <tr>
      <td><div class="clabel">Total pieces</div><div class="cvalue">{{totalQuantity}}</div></td>
      <td><div class="clabel">Total payment</div><div class="cvalue">{{totalEarnings}}</div></td>
      <td><div class="clabel">Products</div><div class="cvalue">{{productsWorkedOn}}</div></td>
    </tr>
  </table>
  <div class="sec">By product</div>
  <table>
    <thead><tr><th>Product</th><th class="num">Pieces</th><th class="num">Payment</th></tr></thead>
    <tbody>{{productRows}}</tbody>
  </table>
  <div class="sec">Daily production</div>
  <table>
    <thead><tr><th>Date</th><th>Product</th><th class="num">Pieces</th><th class="num">Rate</th><th class="num">Payment</th></tr></thead>
    <tbody>{{historyRows}}</tbody>
  </table>
  <div class="foot">Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for the employee date-range report PDF', now(), now());
