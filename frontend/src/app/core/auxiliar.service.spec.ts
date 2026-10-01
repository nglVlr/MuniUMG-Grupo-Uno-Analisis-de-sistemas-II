import { TestBed } from '@angular/core/testing';

import { AuxiliarService } from './auxiliar.service';

describe('AuxiliarService', () => {
  let service: AuxiliarService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuxiliarService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
