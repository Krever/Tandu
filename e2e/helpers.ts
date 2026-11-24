// Helpers shared by the sheet-builder (print page) tests.

/** The "−" and "+" buttons of a stepper. */
export const stepperMinus = (page, testId: string) => page.getByTestId(testId).locator("button").first()
export const stepperPlus = (page, testId: string) => page.getByTestId(testId).locator("button").nth(1)

/** The print copies are built only while printing; this starts that, as
  * Ctrl+P does. */
export const startPrinting = (page) => page.evaluate(() => window.dispatchEvent(new Event("beforeprint")))
