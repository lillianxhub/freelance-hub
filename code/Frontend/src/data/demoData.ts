import type { WorkspaceData } from '../types/domain'

const ownerId = '00000000-0000-4000-8000-000000000001'

export interface DemoUser {
  id: string
  email: string
  full_name: string
  password: string
}

export const demoUser: DemoUser = {
  id: ownerId,
  email: 'demo@freelancehub.test',
  full_name: 'nattadol',
  password: 'demo1234',
}

export const createDemoWorkspace = (): WorkspaceData => ({
  profiles: [
    {
      id: ownerId,
      owner_id: ownerId,
      full_name: 'nattadol sarika ',
      email: 'demo@freelancehub.test',
      phone: '089-123-4567',
      address: '123 Mittraphap Road, Khon Kaen 40000',
      tax_id: '0405566000001',
      bank_name: 'Kasikornbank',
      bank_account_name: 'Petpinyo Nattadol',
      bank_account_number: '123-4-56789-0',
      timezone: 'Asia/Bangkok',
      currency: 'THB',
      date_format: 'DD/MM/YYYY',
      default_tax_rate: 7,
      default_hourly_rate: 850,
      logo_url: '',
    },
  ],
  clients: [
    {
      id: 'client-northstar', owner_id: ownerId, name: 'Maya Chen', company_name: 'Northstar Studio',
      email: 'hello@northstar.studio', phone: '02-123-4567', address: '88 Sukhumvit Road, Bangkok 10110',
      tax_id: '0105564012345', notes: 'Design retainer client. Prefers email updates.', status: 'ACTIVE', color: '#3867f4',
      created_at: '2026-08-10T08:00:00Z', updated_at: '2026-09-24T03:00:00Z',
    },
    {
      id: 'client-goodgoods', owner_id: ownerId, name: 'Ben Walker', company_name: 'Good Goods Co.',
      email: 'team@goodgoods.co', phone: '081-555-0198', address: '20 Nimman Road, Chiang Mai 50200',
      tax_id: '0505567000456', notes: 'Brand identity and launch collateral.', status: 'ACTIVE', color: '#f07b62',
      created_at: '2026-08-16T08:00:00Z', updated_at: '2026-09-23T05:30:00Z',
    },
    {
      id: 'client-orbit', owner_id: ownerId, name: 'Iris Park', company_name: 'Orbit Labs',
      email: 'hello@orbitlabs.io', phone: '086-441-8832', address: '99 Innovation District, Bangkok 10260',
      tax_id: '0105567029184', notes: 'Early-stage product team.', status: 'ACTIVE', color: '#9b7bea',
      created_at: '2026-09-01T08:00:00Z', updated_at: '2026-09-21T12:00:00Z',
    },
    {
      id: 'client-archived', owner_id: ownerId, name: 'Tom Adams', company_name: 'Paper & Co.',
      email: 'tom@paperandco.test', phone: '084-770-1212', address: '14 Old Town, Phuket 83000',
      tax_id: '', notes: 'Completed engagement.', status: 'ARCHIVED', color: '#7c879f',
      created_at: '2026-06-10T08:00:00Z', updated_at: '2026-08-31T12:00:00Z',
    },
  ],
  projects: [
    {
      id: 'project-website', owner_id: ownerId, client_id: 'client-northstar', name: 'Website redesign',
      description: 'Research, UX and visual redesign for the company website.', color: '#3867f4', status: 'ACTIVE',
      billing_type: 'HOURLY', hourly_rate: 950, fixed_price: null, budget_hours: 60, budget_amount: 57000,
      currency: 'THB', start_date: '2026-09-01', end_date: '2026-10-20', created_at: '2026-08-25T08:00:00Z', updated_at: '2026-09-24T04:00:00Z',
    },
    {
      id: 'project-brand', owner_id: ownerId, client_id: 'client-goodgoods', name: 'Brand identity kit',
      description: 'Logo system, color palette and brand usage guide.', color: '#f07b62', status: 'ACTIVE',
      billing_type: 'FIXED_PRICE', hourly_rate: null, fixed_price: 68000, budget_hours: 55, budget_amount: 68000,
      currency: 'THB', start_date: '2026-08-20', end_date: '2026-10-05', created_at: '2026-08-18T08:00:00Z', updated_at: '2026-09-23T05:00:00Z',
    },
    {
      id: 'project-mobile', owner_id: ownerId, client_id: 'client-orbit', name: 'Mobile app concept',
      description: 'Product discovery and high-fidelity onboarding prototype.', color: '#9b7bea', status: 'PLANNED',
      billing_type: 'HOURLY', hourly_rate: 1100, fixed_price: null, budget_hours: 40, budget_amount: 44000,
      currency: 'THB', start_date: '2026-10-01', end_date: '2026-11-15', created_at: '2026-09-10T08:00:00Z', updated_at: '2026-09-21T04:00:00Z',
    },
    {
      id: 'project-campaign', owner_id: ownerId, client_id: 'client-northstar', name: 'Launch campaign',
      description: 'Campaign landing page and social media graphics.', color: '#21a179', status: 'ON_HOLD',
      billing_type: 'FIXED_PRICE', hourly_rate: null, fixed_price: 42000, budget_hours: 35, budget_amount: 42000,
      currency: 'THB', start_date: '2026-08-01', end_date: '2026-09-30', created_at: '2026-07-28T08:00:00Z', updated_at: '2026-09-18T04:00:00Z',
    },
  ],
  tasks: [
    { id: 'task-homepage', owner_id: ownerId, project_id: 'project-website', name: 'Finalize homepage concepts', description: '', status: 'IN_PROGRESS', sort_order: 1, due_date: '2026-09-25' },
    { id: 'task-design-system', owner_id: ownerId, project_id: 'project-website', name: 'Build design system', description: '', status: 'TODO', sort_order: 2, due_date: '2026-09-29' },
    { id: 'task-prototype', owner_id: ownerId, project_id: 'project-website', name: 'Prototype key flows', description: '', status: 'DONE', sort_order: 3, due_date: '2026-09-22' },
    { id: 'task-logo', owner_id: ownerId, project_id: 'project-brand', name: 'Prepare logo review', description: '', status: 'IN_REVIEW', sort_order: 1, due_date: '2026-09-26' },
    { id: 'task-guideline', owner_id: ownerId, project_id: 'project-brand', name: 'Draft brand guideline', description: '', status: 'TODO', sort_order: 2, due_date: '2026-10-01' },
    { id: 'task-onboarding', owner_id: ownerId, project_id: 'project-mobile', name: 'Map onboarding flow', description: '', status: 'TODO', sort_order: 1, due_date: '2026-10-03' },
    { id: 'task-research', owner_id: ownerId, project_id: 'project-mobile', name: 'Competitor research', description: '', status: 'DONE', sort_order: 2, due_date: '2026-09-20' },
  ],
  time_entries: [
    { id: 'time-1', owner_id: ownerId, project_id: 'project-website', task_id: 'task-homepage', description: 'Homepage visual exploration', started_at: '2026-09-24T02:20:00Z', ended_at: '2026-09-24T05:50:00Z', duration_minutes: 210, billable: true, rate_snapshot: 950, currency: 'THB', invoice_id: null, created_at: '2026-09-24T05:50:00Z' },
    { id: 'time-2', owner_id: ownerId, project_id: 'project-brand', task_id: 'task-logo', description: 'Logo refinement', started_at: '2026-09-23T03:00:00Z', ended_at: '2026-09-23T07:15:00Z', duration_minutes: 255, billable: true, rate_snapshot: 0, currency: 'THB', invoice_id: null, created_at: '2026-09-23T07:15:00Z' },
    { id: 'time-3', owner_id: ownerId, project_id: 'project-website', task_id: null, description: 'Client call and planning', started_at: '2026-09-22T07:00:00Z', ended_at: '2026-09-22T07:45:00Z', duration_minutes: 45, billable: false, rate_snapshot: 950, currency: 'THB', invoice_id: null, created_at: '2026-09-22T07:45:00Z' },
    { id: 'time-4', owner_id: ownerId, project_id: 'project-website', task_id: 'task-design-system', description: 'Component inventory', started_at: '2026-09-19T02:00:00Z', ended_at: '2026-09-19T07:30:00Z', duration_minutes: 330, billable: true, rate_snapshot: 950, currency: 'THB', invoice_id: 'invoice-1001', created_at: '2026-09-19T07:30:00Z' },
    { id: 'time-5', owner_id: ownerId, project_id: 'project-campaign', task_id: null, description: 'Campaign direction', started_at: '2026-09-17T03:00:00Z', ended_at: '2026-09-17T06:00:00Z', duration_minutes: 180, billable: true, rate_snapshot: 1200, currency: 'THB', invoice_id: 'invoice-1001', created_at: '2026-09-17T06:00:00Z' },
  ],
  finance_entries: [
    { id: 'finance-1', owner_id: ownerId, project_id: 'project-brand', type: 'EXPENSE', category: 'Software', amount: 1290, currency: 'THB', entry_date: '2026-09-10', notes: 'Font license' },
    { id: 'finance-2', owner_id: ownerId, project_id: null, type: 'INCOME', category: 'Consulting', amount: 7500, currency: 'THB', entry_date: '2026-09-14', notes: 'One-off consultation' },
    { id: 'finance-3', owner_id: ownerId, project_id: 'project-website', type: 'EXPENSE', category: 'Travel', amount: 840, currency: 'THB', entry_date: '2026-09-18', notes: 'Client workshop travel' },
  ],
  invoices: [
    { id: 'invoice-1001', owner_id: ownerId, client_id: 'client-northstar', project_id: 'project-website', invoice_number: 'INV-2026-1001', issue_date: '2026-09-20', due_date: '2026-10-05', status: 'ISSUED', currency: 'THB', subtotal: 8825, discount_amount: 0, tax_rate: 7, tax_amount: 617.75, total: 9442.75, amount_paid: 0, notes: 'Thank you for your business.', seller_snapshot: { name: 'Petpinyo Nattadol', tax_id: '0405566000001' }, client_snapshot: { name: 'Northstar Studio', tax_id: '0105564012345' }, created_at: '2026-09-20T08:00:00Z' },
    { id: 'invoice-1000', owner_id: ownerId, client_id: 'client-goodgoods', project_id: 'project-brand', invoice_number: 'INV-2026-1000', issue_date: '2026-08-28', due_date: '2026-09-12', status: 'PAID', currency: 'THB', subtotal: 34000, discount_amount: 0, tax_rate: 7, tax_amount: 2380, total: 36380, amount_paid: 36380, notes: 'Deposit payment.', seller_snapshot: { name: 'Petpinyo Nattadol', tax_id: '0405566000001' }, client_snapshot: { name: 'Good Goods Co.', tax_id: '0505567000456' }, created_at: '2026-08-28T08:00:00Z' },
  ],
  invoice_items: [
    { id: 'item-1', owner_id: ownerId, invoice_id: 'invoice-1001', time_entry_id: 'time-4', description: 'Website redesign — component inventory', quantity: 5.5, unit_price: 950, amount: 5225, sort_order: 1 },
    { id: 'item-2', owner_id: ownerId, invoice_id: 'invoice-1001', time_entry_id: 'time-5', description: 'Launch campaign — creative direction', quantity: 3, unit_price: 1200, amount: 3600, sort_order: 2 },
    { id: 'item-3', owner_id: ownerId, invoice_id: 'invoice-1000', time_entry_id: null, description: 'Brand identity deposit (50%)', quantity: 1, unit_price: 34000, amount: 34000, sort_order: 1 },
  ],
  payments: [
    { id: 'payment-1', owner_id: ownerId, invoice_id: 'invoice-1000', amount: 36380, currency: 'THB', paid_at: '2026-09-05', method: 'BANK_TRANSFER', notes: 'Full payment' },
  ],
})
