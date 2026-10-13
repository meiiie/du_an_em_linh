import { afterNextRender, DestroyRef, Directive, ElementRef, inject, Injector } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';

/** Chuyển trang nhẹ, tự triển khai; không lấy mã thư viện chuyển động tham chiếu. */
@Directive({ selector: '[appChuyenTrang]' })
export class ChuyenTrang {
  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly injector = inject(Injector);
  private animation?: Animation;
  constructor() {
    inject(Router).events.pipe(takeUntilDestroyed()).subscribe(event => {
      if (!(event instanceof NavigationEnd)) return;
      afterNextRender(() => {
        this.animation?.cancel();
        if (typeof matchMedia !== 'function' || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
        const node = this.el.nativeElement;
        if (typeof node.animate !== 'function') return;
        this.animation = node.animate([{ opacity: 0, transform: 'translateY(6px)' }, { opacity: 1, transform: 'translateY(0)' }], { duration: 180, easing: 'ease-out' });
      }, { injector: this.injector });
    });
    inject(DestroyRef).onDestroy(() => this.animation?.cancel());
  }
}
