export interface printerDevice {
  name: string;
  address: string;
}

export interface usbPrinterDevice {
  id: string;
  name: string;
  productName: string;
  manufacturerName: string;
  // serialNumber: string;
  vendorId: string;
  version: string;
  productId: string;
}
