import { Check } from 'lucide-react'
import { Slider } from 'radix-ui'
import { useRef, useState, type PointerEvent } from 'react'
import { Button } from './button'
import { Input } from './input'
import { Popover, PopoverContent, PopoverTrigger } from './popover'

type Hsv = { hue: number; saturation: number; brightness: number }

const presets = ['#4F6BFF', '#7C3AED', '#0EA5E9', '#10B981', '#F59E0B', '#EF4444', '#EC4899', '#64748B']
const isHex = (value: string) => /^#[0-9a-f]{6}$/i.test(value)
const clamp = (value: number) => Math.max(0, Math.min(1, value))

function hexToHsv(hex: string): Hsv {
  const [red, green, blue] = [1, 3, 5].map((index) => Number.parseInt(hex.slice(index, index + 2), 16) / 255)
  const max = Math.max(red, green, blue)
  const min = Math.min(red, green, blue)
  const difference = max - min
  let hue = 0

  if (difference !== 0) {
    if (max === red) hue = ((green - blue) / difference) % 6
    else if (max === green) hue = (blue - red) / difference + 2
    else hue = (red - green) / difference + 4
    hue = (hue * 60 + 360) % 360
  }

  return { hue, saturation: max === 0 ? 0 : difference / max, brightness: max }
}

function hsvToHex({ hue, saturation, brightness }: Hsv): string {
  const chroma = brightness * saturation
  const intermediate = chroma * (1 - Math.abs((hue / 60) % 2 - 1))
  const offset = brightness - chroma
  const channels = hue < 60 ? [chroma, intermediate, 0]
    : hue < 120 ? [intermediate, chroma, 0]
      : hue < 180 ? [0, chroma, intermediate]
        : hue < 240 ? [0, intermediate, chroma]
          : hue < 300 ? [intermediate, 0, chroma]
            : [chroma, 0, intermediate]

  return `#${channels.map((channel) => Math.round((channel + offset) * 255).toString(16).padStart(2, '0')).join('')}`.toUpperCase()
}

interface ColorPickerProps {
  id?: string
  value: string
  onChange: (value: string) => void
  disabled?: boolean
  'aria-label'?: string
}

export function ColorPicker({ id, value, onChange, disabled, 'aria-label': ariaLabel }: ColorPickerProps) {
  const selectedColor = isHex(value) ? value.toUpperCase() : '#4F6BFF'
  const [hueForGray, setHueForGray] = useState<number | null>(null)
  const [draftHex, setDraftHex] = useState<string | null>(null)
  const areaRef = useRef<HTMLDivElement>(null)
  const hsv = hexToHsv(selectedColor)
  const hue = hsv.saturation === 0 && hueForGray !== null ? hueForGray : hsv.hue
  const visibleHex = draftHex ?? selectedColor

  const chooseColor = (nextColor: string) => {
    setDraftHex(null)
    onChange(nextColor)
  }

  const updateArea = (event: PointerEvent<HTMLDivElement>) => {
    const rectangle = areaRef.current?.getBoundingClientRect()
    if (!rectangle) return
    chooseColor(hsvToHex({
      hue,
      saturation: clamp((event.clientX - rectangle.left) / rectangle.width),
      brightness: 1 - clamp((event.clientY - rectangle.top) / rectangle.height),
    }))
  }

  const handleHexChange = (text: string) => {
    setDraftHex(text)
    const normalized = text.startsWith('#') ? text : `#${text}`
    if (isHex(normalized)) chooseColor(normalized.toUpperCase())
  }

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button id={id} type="button" variant="outline" disabled={disabled} aria-label={ariaLabel ?? 'เลือกสีโปรเจกต์'} className="h-[41px] justify-start gap-2 px-2 font-normal">
          <span className="size-6 rounded-md border border-black/10" style={{ backgroundColor: selectedColor }} aria-hidden="true" />
          <span>{selectedColor}</span>
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-72 space-y-4 p-3" aria-label="เลือกสีโปรเจกต์">
        <div
          ref={areaRef}
          role="slider"
          tabIndex={0}
          aria-label="ความอิ่มตัวและความสว่าง"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={Math.round(hsv.saturation * 100)}
          aria-valuetext={`ความอิ่มตัว ${Math.round(hsv.saturation * 100)}% ความสว่าง ${Math.round(hsv.brightness * 100)}%`}
          className="relative h-40 touch-none cursor-crosshair rounded-lg"
          style={{ background: `linear-gradient(to top, #000, transparent), linear-gradient(to right, #fff, transparent), hsl(${hue} 100% 50%)` }}
          onPointerDown={(event) => {
            event.currentTarget.setPointerCapture(event.pointerId)
            updateArea(event)
          }}
          onPointerMove={(event) => { if (event.currentTarget.hasPointerCapture(event.pointerId)) updateArea(event) }}
          onKeyDown={(event) => {
            const step = event.shiftKey ? 0.1 : 0.01
            const next = { hue, saturation: hsv.saturation, brightness: hsv.brightness }
            if (event.key === 'ArrowRight') next.saturation = clamp(next.saturation + step)
            else if (event.key === 'ArrowLeft') next.saturation = clamp(next.saturation - step)
            else if (event.key === 'ArrowUp') next.brightness = clamp(next.brightness + step)
            else if (event.key === 'ArrowDown') next.brightness = clamp(next.brightness - step)
            else return
            event.preventDefault()
            chooseColor(hsvToHex(next))
          }}
        >
          <span className="pointer-events-none absolute size-4 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white shadow-[0_0_0_1px_rgba(0,0,0,.5)]" style={{ left: `${hsv.saturation * 100}%`, top: `${(1 - hsv.brightness) * 100}%` }} />
        </div>

        <Slider.Root
          aria-label="เฉดสี"
          className="relative flex h-4 w-full touch-none items-center"
          min={0}
          max={360}
          step={1}
          value={[hue]}
          onValueChange={([nextHue]) => {
            setHueForGray(nextHue)
            chooseColor(hsvToHex({ ...hsv, hue: nextHue }))
          }}
        >
          <Slider.Track className="h-3 w-full rounded-full bg-[linear-gradient(to_right,#f00,#ff0,#0f0,#0ff,#00f,#f0f,#f00)]" />
          <Slider.Thumb className="block size-4 rounded-full border border-border bg-background shadow focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" />
        </Slider.Root>

        <div className="relative h-3 rounded-full" aria-label="ความทึบ 100 เปอร์เซ็นต์" style={{ background: `linear-gradient(to right, transparent, ${selectedColor}), repeating-conic-gradient(#ddd 0% 25%, #fff 0% 50%) 0 0 / 8px 8px` }}>
          <span className="absolute top-1/2 right-0 size-4 translate-x-1/2 -translate-y-1/2 rounded-full border border-border bg-background shadow" />
        </div>

        <div className="flex items-center gap-2">
          <span className="flex h-8 w-20 shrink-0 items-center rounded-md border border-input px-3 text-xs">HEX</span>
          <Input
            value={visibleHex}
            onChange={(event) => handleHexChange(event.target.value)}
            onBlur={() => setDraftHex(null)}
            aria-label="รหัสสี HEX"
            aria-invalid={draftHex !== null && !isHex(draftHex.startsWith('#') ? draftHex : `#${draftHex}`)}
            className="h-8 flex-1 px-2 text-xs"
            maxLength={7}
          />
          <span className="flex h-8 w-[52px] shrink-0 items-center justify-center rounded-md border border-input text-xs" aria-label="ความทึบ 100 เปอร์เซ็นต์">100%</span>
        </div>

        <div className="grid grid-cols-4 gap-2 border-t border-border pt-3" aria-label="สีสำเร็จรูป">
          {presets.map((preset) => (
            <Button key={preset} type="button" variant="outline" size="icon" className="relative size-10 p-1" aria-label={`เลือกสี ${preset}`} onClick={() => chooseColor(preset)}>
              <span className="size-full rounded-md" style={{ backgroundColor: preset }} />
              {selectedColor === preset && <Check className="absolute size-4 text-white drop-shadow" aria-hidden="true" />}
            </Button>
          ))}
        </div>
      </PopoverContent>
    </Popover>
  )
}
