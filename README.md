# Manifest-cargo-app

flowchart 
                                [ Mulai ]
                                    │
                                    ▼
                 [ Inisialisasi Data Penerbangan ]
                 - Tanggal: 11/12/2025
                 - Flight No / Reg: 3Y704 / PK-MYE
                 - Rute: DJJ -> WMX (Flight Freq: 2)
                                    │
                                    ▼
                   [ Terima / Input Items Kargo ]
                   (PTI, Item Description, Customer)
                                    │
                                    ▼
                        ( Memiliki Berat Pcs? )
                        ┌───────────┴───────────┐
                       [ Ya ]                 [ Tidak ]
                        │                       │
                        ▼                       ▼
            [ Hitung Sub Total Berat ]     [ Input Total Berat ]
            [ (Pcs x Weight Pcs) ]         [ Sub Total Langsung ]
                        │                       │
                        └───────────┬───────────┘
                                    │
                                    ▼
                       [ Rekap Manifest Cargo ]
                      (Hitung Total Pcs & Weight)
                                    │
                                    ▼
                [ Pengelompokan Stowing / Palletizing ]
                - Kelompokkan barang ke PAG (PAG 0147, PAG 0375, dll)
                - Atau kelompokkan ke Manual Load
                                    │
                                    ▼
                [ Hitung Weight Net & Gross per PAG ]
                - Net Weight = Berat Total Isi Barang
                - Gross Weight = Net Weight + Berat Kontainer/Pallet
                                    │
                                    ▼
                       ( Validasi Total Weight )
                  ┌─────────────────┴─────────────────┐
                  ▼                                   ▼
          ( Total Weight Match? )             ( Tidak Sesuai )
          ┌───────┴───────┐                           │
         [ Ya ]        [ Tidak ] ─────────────────────┘
          │
          ▼
    [ Penandatanganan / Verifikasi ]
    - Prepared by (M Nur Alam)
    - Approved by (FOO Incharge)
          │
          ▼
    [ Penerbitan Final Manifest & Stowing Checklist ]
          │
          ▼
      [ Selesai ]
      
