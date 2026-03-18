import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  inject,
  input,
  OnChanges,
  OnDestroy,
  output,
  PLATFORM_ID,
  signal,
  SimpleChanges,
  viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AspectRatio, CropRect } from '../../../models/crop-image.model';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';

type HandleType =
  | 'tl' | 'tc' | 'tr'
  | 'ml' | 'mr'
  | 'bl' | 'bc' | 'br'
  | 'move';

interface Point { x: number; y: number; }

const HANDLE_SIZE = 10;    // px – half-size for hit testing
const MIN_CROP_PX = 20;    // minimum crop dimension in display pixels

@Component({
  selector: 'app-image-crop-editor',
  imports: [CommonModule, MatIconModule, MatButtonModule, MatTooltipModule],
  templateUrl: './image-crop-editor.component.html',
  styleUrl: './image-crop-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageCropEditorComponent implements AfterViewInit, OnChanges, OnDestroy {
  // ── Inputs / Outputs ────────────────────────────────────────────────────────
  /** Blob URL or data-URL of the image to crop */
  imageSrc = input.required<string>();
  /** Initial aspect ratio */
  aspectRatio = input<AspectRatio>('free');

  /** Emits current crop state whenever the user finishes a drag */
  cropChanged = output<{
    cropRect: CropRect;
    rotation: number;
    flipH: boolean;
    flipV: boolean;
    naturalWidth: number;
    naturalHeight: number;
  }>();

  private canvasRef    = viewChild<ElementRef<HTMLCanvasElement>>('cropCanvas');
  private containerRef = viewChild<ElementRef<HTMLDivElement>>('editorContainer');

  // ── Public state (template-bound) ───────────────────────────────────────────
  rotation     = signal(0);
  flipH        = signal(false);
  flipV        = signal(false);
  cropInfo     = signal<{ w: number; h: number }>({ w: 0, h: 0 });
  isLoaded     = signal(false);
  selectedRatio = signal<AspectRatio>('free');

  readonly ratioOptions: { label: string; value: AspectRatio }[] = [
    { label: 'Free',  value: 'free'  },
    { label: '1:1',   value: '1:1'   },
    { label: '4:3',   value: '4:3'   },
    { label: '16:9',  value: '16:9'  },
    { label: '3:4',   value: '3:4'   },
    { label: '9:16',  value: '9:16'  },
  ];

  // ── Private state ───────────────────────────────────────────────────────────
  private img = new Image();
  private ctx!: CanvasRenderingContext2D;

  /** Natural image dimensions */
  private natW = 0;
  private natH = 0;

  /**
   * Display scale: how many natural pixels per canvas pixel.
   * scale = naturalWidth / canvasWidth (after rotation)
   */
  private scale = 1;

  /** Canvas display dimensions */
  private canvasW = 0;
  private canvasH = 0;

  /** Crop rect in CANVAS (display) pixels */
  private crop: CropRect = { x: 0, y: 0, width: 0, height: 0 };

  // Drag state
  private isDragging   = false;
  private activeHandle: HandleType | null = null;
  private dragStart: Point = { x: 0, y: 0 };
  private cropAtDragStart: CropRect = { x: 0, y: 0, width: 0, height: 0 };

  private resizeObserver: ResizeObserver | null = null;
  private platformId = inject(PLATFORM_ID);
  private cdr        = inject(ChangeDetectorRef);

  // ── Lifecycle ────────────────────────────────────────────────────────────────

  ngAfterViewInit() {
    if (!isPlatformBrowser(this.platformId)) return;
    this.initCanvas();
    this.loadImage();
    this.setupResizeObserver();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['imageSrc'] && !changes['imageSrc'].firstChange) {
      this.rotation.set(0);
      this.flipH.set(false);
      this.flipV.set(false);
      this.isLoaded.set(false);
      this.loadImage();
    }
    if (changes['aspectRatio']) {
      this.selectedRatio.set(this.aspectRatio());
      if (this.isLoaded()) this.resetCropToAspect();
    }
  }

  ngOnDestroy() {
    this.resizeObserver?.disconnect();
  }

  // ── Initialisation ───────────────────────────────────────────────────────────

  private initCanvas() {
    const canvas = this.canvasRef()?.nativeElement;
    if (!canvas) return;
    this.ctx = canvas.getContext('2d')!;
  }

  private loadImage() {
    this.img = new Image();
    this.img.onload = () => {
      this.natW = this.img.naturalWidth;
      this.natH = this.img.naturalHeight;
      this.isLoaded.set(true);
      this.cdr.markForCheck();
      this.resize();
    };
    this.img.onerror = () => console.error('[CropEditor] Failed to load image');
    this.img.src = this.imageSrc();
  }

  private setupResizeObserver() {
    const container = this.containerRef()?.nativeElement;
    if (!container) return;
    this.resizeObserver = new ResizeObserver(() => {
      if (this.isLoaded()) this.resize();
    });
    this.resizeObserver.observe(container);
  }

  // ── Core: resize, draw ───────────────────────────────────────────────────────

  /** Recalculate canvas size based on container width + current rotation */
  resize() {
    const container = this.containerRef()?.nativeElement;
    const canvas    = this.canvasRef()?.nativeElement;
    if (!container || !canvas || !this.natW || !this.natH) return;

    const containerW = container.clientWidth;
    const containerH = container.clientHeight || window.innerHeight * 0.55;

    // After rotation, logical natural dims may swap
    const [logicalNatW, logicalNatH] = this.logicalDimensions();

    // Fit image inside container preserving aspect
    const scaleX = containerW  / logicalNatW;
    const scaleY = containerH / logicalNatH;
    const fit    = Math.min(scaleX, scaleY, 1); // never upscale beyond 1:1 if natural size is smaller

    this.canvasW = Math.round(logicalNatW * fit);
    this.canvasH = Math.round(logicalNatH * fit);
    this.scale   = logicalNatW / this.canvasW; // natural pixels per display pixel

    canvas.width  = this.canvasW;
    canvas.height = this.canvasH;

    // Set canvas CSS size explicitly (avoids DPR blurriness)
    canvas.style.width  = this.canvasW + 'px';
    canvas.style.height = this.canvasH + 'px';

    this.resetCropToAspect();
    this.draw();
    this.emitCrop();
  }

  /** Logical dimensions after rotation (what the user sees) */
  private logicalDimensions(): [number, number] {
    const r = ((this.rotation() % 360) + 360) % 360;
    return (r === 90 || r === 270) ? [this.natH, this.natW] : [this.natW, this.natH];
  }

  draw() {
    if (!this.ctx || !this.canvasW || !this.canvasH) return;
    const ctx = this.ctx;

    ctx.clearRect(0, 0, this.canvasW, this.canvasH);

    // ── Draw transformed image ─────────────────────────────────────────────
    ctx.save();
    ctx.translate(this.canvasW / 2, this.canvasH / 2);

    if (this.flipH() || this.flipV()) {
      ctx.scale(this.flipH() ? -1 : 1, this.flipV() ? -1 : 1);
    }
    ctx.rotate((this.rotation() * Math.PI) / 180);

    // After rotation the image coords: back to natural, draw centred
    const drawW = (this.rotation() === 90 || this.rotation() === 270) ? this.canvasH : this.canvasW;
    const drawH = (this.rotation() === 90 || this.rotation() === 270) ? this.canvasW : this.canvasH;
    ctx.drawImage(this.img, -drawW / 2, -drawH / 2, drawW, drawH);
    ctx.restore();

    // ── Dark overlay outside crop ──────────────────────────────────────────
    ctx.fillStyle = 'rgba(0,0,0,0.5)';
    ctx.fillRect(0, 0, this.canvasW, this.canvasH);

    const { x, y, width: w, height: h } = this.crop;
    // Punch out crop area (destination-out then back to source-over)
    ctx.save();
    ctx.globalCompositeOperation = 'destination-out';
    ctx.fillStyle = 'rgba(0,0,0,1)';
    ctx.fillRect(x, y, w, h);
    ctx.restore();

    // Re-draw the image inside crop region only (crisp, no overlay)
    ctx.save();
    ctx.beginPath();
    ctx.rect(x, y, w, h);
    ctx.clip();
    ctx.translate(this.canvasW / 2, this.canvasH / 2);
    if (this.flipH() || this.flipV()) {
      ctx.scale(this.flipH() ? -1 : 1, this.flipV() ? -1 : 1);
    }
    ctx.rotate((this.rotation() * Math.PI) / 180);
    const drawW2 = (this.rotation() === 90 || this.rotation() === 270) ? this.canvasH : this.canvasW;
    const drawH2 = (this.rotation() === 90 || this.rotation() === 270) ? this.canvasW : this.canvasH;
    ctx.drawImage(this.img, -drawW2 / 2, -drawH2 / 2, drawW2, drawH2);
    ctx.restore();

    // ── Crop border ────────────────────────────────────────────────────────
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth   = 1.5;
    ctx.strokeRect(x, y, w, h);

    // Rule-of-thirds grid
    ctx.strokeStyle = 'rgba(255,255,255,0.3)';
    ctx.lineWidth   = 0.75;
    for (let i = 1; i < 3; i++) {
      ctx.beginPath();
      ctx.moveTo(x + (w / 3) * i, y);
      ctx.lineTo(x + (w / 3) * i, y + h);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(x, y + (h / 3) * i);
      ctx.lineTo(x + w, y + (h / 3) * i);
      ctx.stroke();
    }

    // ── Handles ────────────────────────────────────────────────────────────
    this.drawHandles(ctx, x, y, w, h);

    // Update info signal
    this.cropInfo.set({
      w: Math.round(w * this.scale),
      h: Math.round(h * this.scale),
    });
  }

  private drawHandles(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number) {
    const hs = HANDLE_SIZE;
    const handles: Point[] = [
      { x, y },               // tl
      { x: x + w / 2, y },   // tc
      { x: x + w, y },        // tr
      { x, y: y + h / 2 },   // ml
      { x: x + w, y: y + h / 2 }, // mr
      { x, y: y + h },        // bl
      { x: x + w / 2, y: y + h }, // bc
      { x: x + w, y: y + h }, // br
    ];

    handles.forEach(p => {
      ctx.fillStyle   = '#ffffff';
      ctx.strokeStyle = '#1976d2';
      ctx.lineWidth   = 1.5;
      ctx.beginPath();
      ctx.arc(p.x, p.y, hs / 2 + 2, 0, Math.PI * 2);
      ctx.fill();
      ctx.stroke();
    });
  }

  // ── Crop helpers ─────────────────────────────────────────────────────────────

  resetCropToAspect() {
    const padding = 0; // px
    let cw = this.canvasW - padding * 2;
    let ch = this.canvasH - padding * 2;

    const ratio = this.selectedRatio();
    if (ratio !== 'free') {
      const [rw, rh] = ratio.split(':').map(Number);
      const target   = rw / rh;
      const current  = cw / ch;
      if (current > target) {
        cw = Math.round(ch * target);
      } else {
        ch = Math.round(cw / target);
      }
    }

    this.crop = {
      x: Math.round((this.canvasW - cw) / 2),
      y: Math.round((this.canvasH - ch) / 2),
      width:  cw,
      height: ch,
    };
  }

  /** Convert crop from display px → natural px for backend */
  getCropInNaturalPixels(): CropRect {
    return {
      x:      Math.round(this.crop.x      * this.scale),
      y:      Math.round(this.crop.y      * this.scale),
      width:  Math.round(this.crop.width  * this.scale),
      height: Math.round(this.crop.height * this.scale),
    };
  }

  private emitCrop() {
    const natural = this.getCropInNaturalPixels();
    const [lw, lh] = this.logicalDimensions();
    this.cropChanged.emit({
      cropRect:     natural,
      rotation:     this.rotation(),
      flipH:        this.flipH(),
      flipV:        this.flipV(),
      naturalWidth:  lw,
      naturalHeight: lh,
    });
  }

  // ── Toolbar actions ──────────────────────────────────────────────────────────

  rotateCW() {
    this.rotation.set(((this.rotation() + 90) % 360));
    this.resize();
  }

  rotateCCW() {
    this.rotation.set(((this.rotation() - 90 + 360) % 360));
    this.resize();
  }

  toggleFlipH() {
    this.flipH.set(!this.flipH());
    this.draw();
    this.emitCrop();
  }

  toggleFlipV() {
    this.flipV.set(!this.flipV());
    this.draw();
    this.emitCrop();
  }

  reset() {
    this.rotation.set(0);
    this.flipH.set(false);
    this.flipV.set(false);
    this.selectedRatio.set('free');
    this.resize();
  }

  setAspectRatio(ratio: AspectRatio) {
    this.selectedRatio.set(ratio);
    this.resetCropToAspect();
    this.draw();
    this.emitCrop();
  }

  // ── Pointer / Touch events ───────────────────────────────────────────────────

  private getCanvasPoint(event: MouseEvent | Touch): Point {
    const canvas = this.canvasRef()?.nativeElement;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    return {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top,
    };
  }

  private getHandle(p: Point): HandleType | null {
    const { x, y, width: w, height: h } = this.crop;
    const hs = HANDLE_SIZE + 4; // slightly larger hit area for touch
    const corners: [HandleType, Point][] = [
      ['tl', { x,         y         }],
      ['tc', { x: x+w/2,  y         }],
      ['tr', { x: x+w,    y         }],
      ['ml', { x,         y: y+h/2  }],
      ['mr', { x: x+w,    y: y+h/2  }],
      ['bl', { x,         y: y+h    }],
      ['bc', { x: x+w/2,  y: y+h    }],
      ['br', { x: x+w,    y: y+h    }],
    ];

    for (const [type, cp] of corners) {
      if (Math.abs(p.x - cp.x) <= hs && Math.abs(p.y - cp.y) <= hs) return type;
    }

    // Inside crop = move
    if (p.x >= x && p.x <= x + w && p.y >= y && p.y <= y + h) return 'move';
    return null;
  }

  onMouseDown(event: MouseEvent) {
    event.preventDefault();
    const p = this.getCanvasPoint(event);
    this.startDrag(p);
  }

  onMouseMove(event: MouseEvent) {
    if (!this.isDragging) {
      this.updateCursor(this.getCanvasPoint(event));
      return;
    }
    event.preventDefault();
    this.doDrag(this.getCanvasPoint(event));
  }

  @HostListener('window:mouseup', ['$event'])
  onMouseUp(event: MouseEvent) {
    if (this.isDragging) {
      this.endDrag();
    }
  }

  onTouchStart(event: TouchEvent) {
    event.preventDefault();
    const touch = event.touches[0];
    this.startDrag(this.getCanvasPoint(touch));
  }

  onTouchMove(event: TouchEvent) {
    event.preventDefault();
    const touch = event.touches[0];
    this.doDrag(this.getCanvasPoint(touch));
  }

  onTouchEnd() {
    this.endDrag();
  }

  private startDrag(p: Point) {
    const handle = this.getHandle(p);
    if (!handle) return;
    this.isDragging        = true;
    this.activeHandle      = handle;
    this.dragStart         = p;
    this.cropAtDragStart   = { ...this.crop };
  }

  private doDrag(p: Point) {
    if (!this.isDragging || !this.activeHandle) return;

    const dx = p.x - this.dragStart.x;
    const dy = p.y - this.dragStart.y;
    const c  = { ...this.cropAtDragStart };
    const ratio = this.selectedRatio();

    switch (this.activeHandle) {
      case 'move':
        c.x = Math.max(0, Math.min(c.x + dx, this.canvasW - c.width));
        c.y = Math.max(0, Math.min(c.y + dy, this.canvasH - c.height));
        break;
      case 'tl': this.resizeTL(c, dx, dy, ratio); break;
      case 'tc': this.resizeTC(c, dy, ratio);      break;
      case 'tr': this.resizeTR(c, dx, dy, ratio);  break;
      case 'ml': this.resizeML(c, dx, ratio);      break;
      case 'mr': this.resizeMR(c, dx, ratio);      break;
      case 'bl': this.resizeBL(c, dx, dy, ratio);  break;
      case 'bc': this.resizeBC(c, dy, ratio);      break;
      case 'br': this.resizeBR(c, dx, dy, ratio);  break;
    }

    this.crop = c;
    this.draw();
  }

  private endDrag() {
    this.isDragging   = false;
    this.activeHandle = null;
    this.emitCrop();
  }

  // ── Resize handle logic ───────────────────────────────────────────────────────

  private clamp(v: number, min: number, max: number) { return Math.max(min, Math.min(max, v)); }

  private applyAspect(w: number, h: number, ratio: AspectRatio, anchor: 'w' | 'h'): [number, number] {
    if (ratio === 'free') return [w, h];
    const [rw, rh] = ratio.split(':').map(Number);
    const r = rw / rh;
    if (anchor === 'w') return [w, Math.round(w / r)];
    return [Math.round(h * r), h];
  }

  private resizeTL(c: CropRect, dx: number, dy: number, ratio: AspectRatio) {
    let newW = c.width - dx;
    let newH = c.height - dy;
    [newW, newH] = this.applyAspect(newW, newH, ratio, 'w');
    newW = this.clamp(newW, MIN_CROP_PX, c.x + c.width);
    newH = this.clamp(newH, MIN_CROP_PX, c.y + c.height);
    c.x = c.x + c.width - newW;
    c.y = c.y + c.height - newH;
    c.width = newW; c.height = newH;
  }

  private resizeTC(c: CropRect, dy: number, ratio: AspectRatio) {
    let newH = c.height - dy;
    let newW = c.width;
    [newW, newH] = this.applyAspect(newW, newH, ratio, 'h');
    newH = this.clamp(newH, MIN_CROP_PX, c.y + c.height);
    c.y = c.y + c.height - newH;
    c.height = newH;
    if (ratio !== 'free') { c.x = this.clamp(c.x - (newW - c.width) / 2, 0, this.canvasW - newW); c.width = newW; }
  }

  private resizeTR(c: CropRect, dx: number, dy: number, ratio: AspectRatio) {
    let newW = c.width + dx;
    let newH = c.height - dy;
    [newW, newH] = this.applyAspect(newW, newH, ratio, 'w');
    newW = this.clamp(newW, MIN_CROP_PX, this.canvasW - c.x);
    newH = this.clamp(newH, MIN_CROP_PX, c.y + c.height);
    c.y  = c.y + c.height - newH;
    c.width = newW; c.height = newH;
  }

  private resizeML(c: CropRect, dx: number, ratio: AspectRatio) {
    let newW = c.width - dx;
    let newH = c.height;
    [newW, newH] = this.applyAspect(newW, newH, ratio, 'w');
    newW = this.clamp(newW, MIN_CROP_PX, c.x + c.width);
    c.x = c.x + c.width - newW;
    c.width = newW;
    if (ratio !== 'free') { c.y = this.clamp(c.y - (newH - c.height) / 2, 0, this.canvasH - newH); c.height = newH; }
  }

  private resizeMR(c: CropRect, dx: number, ratio: AspectRatio) {
    let newW = c.width + dx;
    let newH = c.height;
    [newW, newH] = this.applyAspect(newW, newH, ratio, 'w');
    newW = this.clamp(newW, MIN_CROP_PX, this.canvasW - c.x);
    c.width = newW;
    if (ratio !== 'free') { c.y = this.clamp(c.y - (newH - c.height) / 2, 0, this.canvasH - newH); c.height = newH; }
  }

  private resizeBL(c: CropRect, dx: number, dy: number, ratio: AspectRatio) {
    let newW = c.width - dx;
    let newH = c.height + dy;
    [newW, newH] = this.applyAspect(newW, newH, ratio, 'w');
    newW = this.clamp(newW, MIN_CROP_PX, c.x + c.width);
    newH = this.clamp(newH, MIN_CROP_PX, this.canvasH - c.y);
    c.x = c.x + c.width - newW;
    c.width = newW; c.height = newH;
  }

  private resizeBC(c: CropRect, dy: number, ratio: AspectRatio) {
    let newH = c.height + dy;
    let newW = c.width;
    [newW, newH] = this.applyAspect(newW, newH, ratio, 'h');
    newH = this.clamp(newH, MIN_CROP_PX, this.canvasH - c.y);
    c.height = newH;
    if (ratio !== 'free') { c.x = this.clamp(c.x - (newW - c.width) / 2, 0, this.canvasW - newW); c.width = newW; }
  }

  private resizeBR(c: CropRect, dx: number, dy: number, ratio: AspectRatio) {
    let newW = c.width + dx;
    let newH = c.height + dy;
    [newW, newH] = this.applyAspect(newW, newH, ratio, 'w');
    newW = this.clamp(newW, MIN_CROP_PX, this.canvasW - c.x);
    newH = this.clamp(newH, MIN_CROP_PX, this.canvasH - c.y);
    c.width = newW; c.height = newH;
  }

  // ── Cursor helper ─────────────────────────────────────────────────────────────

  private updateCursor(p: Point) {
    const canvas = this.canvasRef()?.nativeElement;
    if (!canvas) return;
    const h = this.getHandle(p);
    const cursors: Record<HandleType, string> = {
      tl: 'nw-resize', tc: 'n-resize',  tr: 'ne-resize',
      ml: 'w-resize',                   mr: 'e-resize',
      bl: 'sw-resize', bc: 's-resize',  br: 'se-resize',
      move: 'move',
    };
    canvas.style.cursor = h ? cursors[h] : 'default';
  }


  getCurrentRotation()   { return this.rotation(); }
  getCurrentFlipH()      { return this.flipH(); }
  getCurrentFlipV()      { return this.flipV(); }
}