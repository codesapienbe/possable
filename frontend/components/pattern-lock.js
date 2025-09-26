(function (){
  class PatternLock extends HTMLElement {
    constructor(){
      super();
      this._pattern = [];
      this._pulses = [];
      this._shadow = this.attachShadow({mode:'open'});
      this._canvas = document.createElement('canvas');
      this._shadow.appendChild(this._canvas);
      this._ctx = null;
      this._drawing = false;
      this._hoverIdx = null;
      this._grid = 3;
      this._initResizeObserver();
    }

    connectedCallback(){
      this._setupCanvas();
      this._installHandlers();
      this._render();
    }

    disconnectedCallback(){
      // noop
    }

    get pattern(){ return JSON.stringify(this._pattern); }
    set pattern(v){ this._pattern = v; }

    // public API callable from server
    clearPattern(){ this._pattern = []; this._dispatchChange(); this._render(); }

    _dispatchChange(){
      this.dispatchEvent(new CustomEvent('pattern-changed', { detail: { pattern: this._pattern } }));
      // also keep property for server side reads
      this.setAttribute('pattern', JSON.stringify(this._pattern));
    }

    _initResizeObserver(){
      const ro = new ResizeObserver(()=> this._setupCanvas());
      ro.observe(this);
    }

    _setupCanvas(){
      const sizeAttr = this.getAttribute('size');
      const s = sizeAttr ? parseInt(sizeAttr,10) : Math.min(260, Math.max(140, this.clientWidth || 260));
      this._canvas.width = s;
      this._canvas.height = s;
      this._ctx = this._canvas.getContext('2d');
      this._computeDots();
      this._render();
    }

    _computeDots(){
      const size = this._canvas.width;
      const grid = this._grid;
      const spacing = size/(grid+1);
      this._dots = [];
      for(let r=0;r<grid;r++){
        for(let c=0;c<grid;c++){
          this._dots.push({ x: (c+1)*spacing, y: (r+1)*spacing, idx: r*grid+c });
        }
      }
    }

    _installHandlers(){
      const c = this._canvas;
      c.style.touchAction = 'none';
      c.addEventListener('pointerdown', this._onPointerDown.bind(this));
      c.addEventListener('pointermove', this._onPointerMove.bind(this));
      c.addEventListener('pointerup', this._onPointerUp.bind(this));
      c.addEventListener('pointerleave', this._onPointerUp.bind(this));
    }

    _nearestDot(pos){
      let best = null; let bestd = Infinity;
      for(const d of this._dots){ const dx = d.x-pos.x; const dy = d.y-pos.y; const dist = Math.hypot(dx,dy); if(dist<24 && dist<bestd){ best = d; bestd = dist; } }
      return best;
    }

    _onPointerDown(e){
      this._canvas.setPointerCapture(e.pointerId);
      this._drawing = true;
      const pos = { x: e.offsetX, y: e.offsetY };
      const nearest = this._nearestDot(pos);
      if(nearest && !this._pattern.includes(nearest.idx)){
        this._pattern.push(nearest.idx);
        this._dispatchChange();
        this._pulseAt(nearest.x, nearest.y);
        this._render();
      }
    }

    _onPointerMove(e){
      const pos = { x: e.offsetX, y: e.offsetY };
      const nearest = this._nearestDot(pos);
      if(!this._drawing){
        if(nearest && this._hoverIdx !== nearest.idx){ this._hoverIdx = nearest.idx; this._render(); }
        if(!nearest && this._hoverIdx !== null){ this._hoverIdx = null; this._render(); }
        return;
      }
      if(this._drawing){
        if(nearest && !this._pattern.includes(nearest.idx)){
          this._pattern.push(nearest.idx);
          this._dispatchChange();
          this._pulseAt(nearest.x, nearest.y);
          this._render();
        }
      }
    }

    _onPointerUp(e){
      this._drawing = false;
      this._render();
    }

    _pulseAt(x,y){
      this._pulses.push({ x, y, start: performance.now(), dur: 420 });
      if(!this._pulseTicking){ this._pulseTicking = true; requestAnimationFrame(this._animatePulses.bind(this)); }
    }

    _animatePulses(){
      if(!this._ctx) return;
      const now = performance.now();
      // render base and pattern first
      this._render(true); // skip clearing pulses
      for(let i = this._pulses.length-1; i>=0; i--){ const p = this._pulses[i]; const t = (now - p.start)/p.dur; if(t>=1){ this._pulses.splice(i,1); continue; } const radius = 10 + 20*t; const alpha = Math.max(0, 0.24*(1-t)); this._ctx.beginPath(); this._ctx.fillStyle = 'rgba(255,255,255,' + alpha + ')'; this._ctx.arc(p.x,p.y,radius,0,2*Math.PI); this._ctx.fill(); }
      if(this._pulses.length>0){ requestAnimationFrame(this._animatePulses.bind(this)); } else { this._pulseTicking = false; }
    }

    _render(skipClearPulses){
      if(!this._ctx) return;
      const ctx = this._ctx; const size = this._canvas.width;
      ctx.clearRect(0,0,size,size);
      const cs = getComputedStyle(this);
      const dotBg = (cs.getPropertyValue('--pattern-dot-bg') || 'rgba(255,255,255,0.08)').trim();
      const strokeCol = (cs.getPropertyValue('--pattern-stroke-color') || 'white').trim();
      const dotFill = (cs.getPropertyValue('--pattern-dot-color') || cs.getPropertyValue('--pos-accent') || '#0ea5a4').trim();
      // draw dots
      for(const d of this._dots){
        ctx.beginPath();
        ctx.fillStyle = dotBg;
        ctx.arc(d.x,d.y,8,0,2*Math.PI);
        ctx.fill();
      }
      // draw path
      if(this._pattern && this._pattern.length>0){
        ctx.strokeStyle = strokeCol;
        ctx.lineWidth = 6;
        ctx.beginPath();
        const first = this._dots[this._pattern[0]];
        ctx.moveTo(first.x, first.y);
        for(let i=1;i<this._pattern.length;i++){ const p = this._dots[this._pattern[i]]; ctx.lineTo(p.x,p.y); }
        ctx.stroke();
        for(const idx of this._pattern){ const d = this._dots[idx]; ctx.beginPath(); ctx.fillStyle = dotFill || '#0ea5a4'; ctx.arc(d.x,d.y,10,0,2*Math.PI); ctx.fill(); }
      }
      // hover highlight
      if(this._hoverIdx !== null){ const d = this._dots[this._hoverIdx]; const col = cs.getPropertyValue('--pattern-hover-color') || 'rgba(255,255,255,0.6)'; const r = parseFloat(cs.getPropertyValue('--pattern-hover-radius')) || 14; ctx.beginPath(); ctx.strokeStyle = col.trim(); ctx.lineWidth = 2; ctx.arc(d.x,d.y,r,0,2*Math.PI); ctx.stroke(); }
    }
  }

  customElements.define('pattern-lock', PatternLock);
})(); 