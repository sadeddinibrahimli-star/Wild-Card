import { useCallback, useEffect, useRef, useState } from 'react'
import Icon from './Icon'

/**
 * Cropping the profile picture.
 *
 * The picture used to be cropped automatically with "cover" and the user
 * had no say. Here the user:
 *    - drags the picture to move the frame
 *    - zooms with the slider
 *    - saves the crop with "Use this picture"
 *
 * The result comes back as a 512x512 JPEG blob (that is what is sent to the server).
 */

const SIZE = 1024     // size of the cropped output (so it stays sharp on the profile card)
const FRAME = 280     // frame size on screen

export default function AvatarCrop({ file, onCancel, onDone, busy }) {
  const [img, setImg] = useState(null)
  const [zoom, setZoom] = useState(1)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const [dragging, setDragging] = useState(false)
  const canvasRef = useRef(null)
  const dragStart = useRef(null)
  const imgRef = useRef(null)
  const urlRef = useRef(null)

  useEffect(() => {
    if (!file) return
    const url = URL.createObjectURL(file)
    urlRef.current = url
    const image = new Image()
    image.onload = () => setImg(image)
    image.src = url
    return () => {
      URL.revokeObjectURL(url)
      urlRef.current = null
    }
  }, [file])

  const minZoom = useCallback(() => {
    if (!img) return 1
    return Math.max(FRAME / img.width, FRAME / img.height)
  }, [img])

  const limit = useCallback(() => {
    if (!img) return 0
    const scale = minZoom() * zoom
    const w = img.width * scale
    const h = img.height * scale
    return Math.max(0, (w - FRAME) / 2)
  }, [img, minZoom, zoom])

  useEffect(() => {
    if (!img) return
    setZoom(1)
    setOffset({ x: 0, y: 0 })
  }, [img])

  const onPointerDown = (e) => {
    e.currentTarget.setPointerCapture(e.pointerId)
    dragStart.current = { px: e.clientX, py: e.clientY, ox: offset.x, oy: offset.y }
    setDragging(true)
  }

  const onPointerMove = (e) => {
    if (!dragStart.current) return
    const lim = limit()
    const dx = e.clientX - dragStart.current.px + dragStart.current.ox
    const dy = e.clientY - dragStart.current.py + dragStart.current.oy
    setOffset({
      x: Math.max(-lim, Math.min(lim, dx)),
      y: Math.max(-lim, Math.min(lim, dy)),
    })
  }

  const onPointerUp = () => {
    dragStart.current = null
    setDragging(false)
  }

  useEffect(() => {
    if (!img) return
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, FRAME, FRAME)

    const scale = minZoom() * zoom
    const w = img.width * scale
    const h = img.height * scale

    // the profile picture must stay smooth - no pixelation
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'
    ctx.drawImage(img, (FRAME - w) / 2 + offset.x, (FRAME - h) / 2 + offset.y, w, h)

    imgRef.current = canvas
  }, [img, zoom, offset, minZoom])

  const confirm = () => {
    const src = imgRef.current
    if (!src) return
    const out = document.createElement('canvas')
    out.width = SIZE
    out.height = SIZE
    const ctx = out.getContext('2d')
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'
    ctx.drawImage(src, 0, 0, SIZE, SIZE)

    out.toBlob(
      (blob) => {
        if (!blob) return
        const cropped = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
        onDone(cropped)
      },
      'image/jpeg',
      0.92,
    )
  }

  const reset = () => {
    setZoom(1)
    setOffset({ x: 0, y: 0 })
  }

  return (
    <div className="crop-dim" role="dialog" aria-label="Crop profile picture">
      <div className="crop-modal card">
        <div className="row between">
          <h3>Crop profile picture</h3>
          <button className="wl-del" onClick={onCancel} title="Cancel">
            <Icon name="close" size={14} />
          </button>
        </div>

        <p className="muted small">
          Drag the picture to choose what stays in frame. Zoom in if you need.
        </p>

        <div
          className={'crop-frame' + (dragging ? ' dragging' : '')}
          style={{ width: FRAME, height: FRAME }}
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={onPointerUp}
          onPointerCancel={onPointerUp}
        >
          {!img ? (
            <span className="muted small">Loading...</span>
          ) : (
            <canvas ref={canvasRef} width={FRAME} height={FRAME} />
          )}
          <span className="crop-ring" />
          <span className="crop-grid" />
        </div>

        <div className="row between crop-tools">
          <button
            className="btn ghost"
            onClick={() => setZoom((z) => Math.max(1, +(z - 0.15).toFixed(2)))}
            disabled={!img || zoom <= 1}
            title="Zoom out"
          >
            −
          </button>

          <input
            type="range"
            min="1"
            max="3"
            step="0.05"
            value={zoom}
            onChange={(e) => setZoom(+e.target.value)}
            disabled={!img}
            aria-label="Zoom"
          />

          <button
            className="btn ghost"
            onClick={() => setZoom((z) => Math.min(3, +(z + 0.15).toFixed(2)))}
            disabled={!img}
            title="Zoom in"
          >
            +
          </button>

          <button className="btn ghost" onClick={reset} disabled={!img}>
            Reset
          </button>
        </div>

        <div className="row">
          <button className="btn" onClick={confirm} disabled={!img || busy}>
            {busy ? 'Uploading...' : 'Use this picture'}
          </button>
          <button className="btn ghost" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  )
}