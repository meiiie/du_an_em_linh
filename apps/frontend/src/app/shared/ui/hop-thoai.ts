import { Component, DestroyRef, DOCUMENT, effect, ElementRef, inject, input, output, viewChild } from '@angular/core';
/** Native modal supplies focus containment, inert background and Escape semantics. */
let so=0; let soKhoa=0; let cuonCu='';
@Component({selector:'app-hop-thoai',template:`<dialog #hop class="hop-thoai" [attr.aria-labelledby]="id" (cancel)="huy($event)" (click)="nen($event)"><header><h2 [id]="id">{{tieuDe()}}</h2><button type="button" class="nut-icon" aria-label="Đóng hộp thoại" [disabled]="ban()" (click)="dong.emit()">×</button></header><div class="hop-noi-dung"><ng-content /></div></dialog>`})
export class HopThoai {
 readonly mo=input(false);readonly tieuDe=input.required<string>();readonly ban=input(false);readonly dong=output<void>();
 protected readonly id='hop-'+(++so);private readonly hop=viewChild<ElementRef<HTMLDialogElement>>('hop');private readonly doc=inject(DOCUMENT);
 private khoa=false;private truoc:HTMLElement|null=null;
 constructor(){effect(()=>{const d=this.hop()?.nativeElement;if(!d)return;if(this.mo()&&!d.open){this.truoc=this.doc.activeElement as HTMLElement;if(soKhoa++===0){cuonCu=this.doc.body.style.overflow;this.doc.body.style.overflow='hidden';}this.khoa=true;d.showModal();}else if(!this.mo()&&d.open){d.close();this.moKhoa();this.truoc?.focus();}});inject(DestroyRef).onDestroy(()=>{const d=this.hop()?.nativeElement;if(d?.open)d.close();this.moKhoa();this.truoc?.focus();});}
 private moKhoa(){if(this.khoa){this.khoa=false;if(--soKhoa===0)this.doc.body.style.overflow=cuonCu;}}
 protected huy(e:Event){e.preventDefault();if(!this.ban())this.dong.emit();}
 protected nen(e:MouseEvent){const d=this.hop()?.nativeElement;if(e.target!==d||this.ban())return;const r=d.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)this.dong.emit();}
}
