export type ApiOptions = Omit<RequestInit, 'method' | 'body'>

export type JsonMethod = 'POST' | 'PUT' | 'PATCH'
