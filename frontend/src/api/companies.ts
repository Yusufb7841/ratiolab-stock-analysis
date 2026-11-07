import { getJson } from './client.ts'

export type Company = {
  ticker: string
}

export function fetchCompanies(): Promise<Company[]> {
  return getJson<Company[]>('/api/v1/companies')
}
