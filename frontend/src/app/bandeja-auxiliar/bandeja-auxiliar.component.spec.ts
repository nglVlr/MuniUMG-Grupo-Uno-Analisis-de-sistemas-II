import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BandejaAuxiliarComponent } from './bandeja-auxiliar.component';

describe('BandejaAuxiliarComponent', () => {
  let component: BandejaAuxiliarComponent;
  let fixture: ComponentFixture<BandejaAuxiliarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BandejaAuxiliarComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(BandejaAuxiliarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
