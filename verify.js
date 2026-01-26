import { chromium } from 'playwright';

async function verify() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  try {
    await page.goto('http://localhost:3000/');
    await page.waitForTimeout(2000); // Wait for redirect or load
    await page.screenshot({ path: 'verification.png' });
    console.log('Screenshot taken');
  } catch (e) {
    console.error(e);
  } finally {
    await browser.close();
  }
}

verify();
