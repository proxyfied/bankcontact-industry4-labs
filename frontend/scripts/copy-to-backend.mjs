import { cp, mkdir, rm } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
const source = fileURLToPath(new URL('../dist/', import.meta.url))
const target = fileURLToPath(new URL('../../src/main/resources/static/', import.meta.url))
await rm(target, { recursive: true, force: true })
await mkdir(target, { recursive: true })
await cp(source, target, { recursive: true })
console.log('Vue build copied to Spring Boot static resources')
