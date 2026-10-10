import assert from 'node:assert/strict'
import { JSDOM } from 'jsdom'
import {
  act,
  createElement,
  type ComponentType,
  type PropsWithChildren,
  type ReactNode,
} from 'react'
import { createRoot } from 'react-dom/client'

const dom = new JSDOM('<!doctype html><html><body></body></html>', { url: 'http://localhost' })
Object.assign(globalThis, {
  window: dom.window,
  document: dom.window.document,
  HTMLElement: dom.window.HTMLElement,
  HTMLFormElement: dom.window.HTMLFormElement,
  HTMLInputElement: dom.window.HTMLInputElement,
  HTMLSelectElement: dom.window.HTMLSelectElement,
  HTMLOptionElement: dom.window.HTMLOptionElement,
  Node: dom.window.Node,
  Element: dom.window.Element,
  DocumentFragment: dom.window.DocumentFragment,
  SVGElement: dom.window.SVGElement,
  getComputedStyle: dom.window.getComputedStyle,
  Event: dom.window.Event,
  CustomEvent: dom.window.CustomEvent,
  MutationObserver: dom.window.MutationObserver,
  IS_REACT_ACT_ENVIRONMENT: true,
})

export function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason: unknown) => void
  const promise = new Promise<T>((accept, decline) => {
    resolve = accept
    reject = decline
  })
  return { promise, resolve, reject }
}

export async function renderHook<T>(hook: () => T, Wrapper?: ComponentType<PropsWithChildren>) {
  const container = document.createElement('div')
  document.body.append(container)
  const root = createRoot(container)
  let current: T | undefined
  function Consumer() {
    current = hook()
    return null
  }
  const render = () =>
    root.render(
      Wrapper ? createElement(Wrapper, null, createElement(Consumer)) : createElement(Consumer),
    )
  await act(async () => {
    render()
  })
  return {
    get current() {
      assert.notEqual(current, undefined)
      return current as T
    },
    async rerender(nextHook = hook) {
      hook = nextHook
      await act(async () => {
        render()
      })
    },
    async unmount() {
      await act(async () => {
        root.unmount()
      })
      container.remove()
    },
  }
}

export { act }

export async function renderComponent(element: ReactNode) {
  const container = document.createElement('div')
  document.body.append(container)
  const root = createRoot(container)
  await act(async () => {
    root.render(element)
  })
  return {
    container,
    async unmount() {
      await act(async () => {
        root.unmount()
      })
      container.remove()
    },
  }
}
