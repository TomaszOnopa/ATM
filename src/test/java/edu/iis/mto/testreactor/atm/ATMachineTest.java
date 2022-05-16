package edu.iis.mto.testreactor.atm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.iis.mto.testreactor.atm.bank.AccountException;
import edu.iis.mto.testreactor.atm.bank.AuthorizationException;
import edu.iis.mto.testreactor.atm.bank.Bank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ATMachineTest {
    @Mock
    private Bank bank;
    private ATMachine aTMachine;
    private Currency currency = Money.DEFAULT_CURRENCY;
    private Withdrawal expectedWithdrawal;
    private MoneyDeposit expectedDeposit;
    private PinCode pinCode;
    private Card card;
    private Money amount;

    @BeforeEach
    void setUp() {
        aTMachine = new ATMachine(bank, currency);
        aTMachine.setDeposit(defaultDeposit());

        pinCode = PinCode.createPIN(0, 0, 0, 0);
        card = Card.create("1234");
        amount = new Money(3000, Money.DEFAULT_CURRENCY);
        expectedWithdrawal = defaultWithdrawal();
        expectedDeposit = defaultDepositAfterWithdrawal();
    }

    @Test
    void successfulWithdrawal() throws ATMOperationException {
        Withdrawal result = aTMachine.withdraw(pinCode, card, amount);
        assertEquals(expectedWithdrawal, result);
        assertEquals(expectedDeposit, aTMachine.getCurrentDeposit());
    }

    @Test
    void aTMDoEverythingInProperOrder() throws ATMOperationException, AccountException, AuthorizationException {
        aTMachine.withdraw(pinCode, card, amount);

        InOrder callOrder = Mockito.inOrder(bank);
        callOrder.verify(bank).autorize(Mockito.any(), Mockito.any());
        callOrder.verify(bank).charge(Mockito.any(), Mockito.any());
    }

    @Test
    void amountBiggerThanDepositResultInATMOperationException() {
        amount = new Money(10000, Money.DEFAULT_CURRENCY);

        ATMOperationException exception = assertThrows(ATMOperationException.class, ()->{aTMachine.withdraw(pinCode, card, amount);});
        assertEquals(ErrorCode.WRONG_AMOUNT, exception.getErrorCode());
        assertEquals(defaultDeposit(), aTMachine.getCurrentDeposit());
    }

    @Test
    void differentCurrencyResultInATMOperationException() {
        amount = new Money(1000, Currency.getInstance("USD"));

        ATMOperationException exception = assertThrows(ATMOperationException.class, ()->{aTMachine.withdraw(pinCode, card, amount);});
        assertEquals(ErrorCode.WRONG_CURRENCY, exception.getErrorCode());
        assertEquals(defaultDeposit(), aTMachine.getCurrentDeposit());
    }

    @Test
    void authorizationErrorResultInATMOperationException() throws AuthorizationException {
        Mockito.doThrow(AuthorizationException.class).when(bank).autorize(Mockito.any(), Mockito.any());

        ATMOperationException exception = assertThrows(ATMOperationException.class, ()->{aTMachine.withdraw(pinCode, card, amount);});
        assertEquals(ErrorCode.AHTHORIZATION, exception.getErrorCode());
        assertEquals(defaultDeposit(), aTMachine.getCurrentDeposit());
    }

    @Test
    void accountErrorResultInATMOperationException() throws AccountException {
        Mockito.doThrow(AccountException.class).when(bank).charge(Mockito.any(), Mockito.any());

        ATMOperationException exception = assertThrows(ATMOperationException.class, ()->{aTMachine.withdraw(pinCode, card, amount);});
        assertEquals(ErrorCode.NO_FUNDS_ON_ACCOUNT, exception.getErrorCode());
        assertEquals(defaultDeposit(), aTMachine.getCurrentDeposit());
    }

    @Test
    void amountNotIntegerResultInATMOperationException() {
        amount = new Money(100.50, Money.DEFAULT_CURRENCY);

        ATMOperationException exception = assertThrows(ATMOperationException.class, ()->{aTMachine.withdraw(pinCode, card, amount);});
        assertEquals(ErrorCode.WRONG_AMOUNT, exception.getErrorCode());
        assertEquals(defaultDeposit(), aTMachine.getCurrentDeposit());
    }

    private Withdrawal defaultWithdrawal() {
        List<BanknotesPack> banknotes = new ArrayList<>();
        banknotes.add(BanknotesPack.create(5, Banknote.PL_500));
        banknotes.add(BanknotesPack.create(2, Banknote.PL_200));
        banknotes.add(BanknotesPack.create(1, Banknote.PL_100));

        return Withdrawal.create(banknotes);
    }

    private MoneyDeposit defaultDeposit() {
        List<BanknotesPack> banknotes = new ArrayList<>();
        banknotes.add(BanknotesPack.create(5, Banknote.PL_500));
        banknotes.add(BanknotesPack.create(5, Banknote.PL_200));
        banknotes.add(BanknotesPack.create(5, Banknote.PL_100));
        return MoneyDeposit.create(Money.DEFAULT_CURRENCY, banknotes);
    }

    private MoneyDeposit defaultDepositAfterWithdrawal() {
        List<BanknotesPack> banknotes = new ArrayList<>();
        banknotes.add(BanknotesPack.create(0, Banknote.PL_500));
        banknotes.add(BanknotesPack.create(3, Banknote.PL_200));
        banknotes.add(BanknotesPack.create(4, Banknote.PL_100));
        return MoneyDeposit.create(Money.DEFAULT_CURRENCY, banknotes);
    }
}
