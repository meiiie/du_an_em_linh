import { TestBed } from '@angular/core/testing';
import { PreloadAllModules, PreloadingStrategy } from '@angular/router';
import { appConfig } from './app.config';

describe('Nạp trước trang học', () => {
  it('nạp trước các trang lười để giảm thời gian chờ lần vào học đầu tiên', () => {
    TestBed.configureTestingModule({providers:appConfig.providers});
    expect(TestBed.inject(PreloadingStrategy)).toBeInstanceOf(PreloadAllModules);
  });
});
